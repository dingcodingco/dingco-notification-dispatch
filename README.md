# dingco-notification-dispatch

딩코딩코 칸 강의 「1억 건 알림 발송 시스템 설계: Claude Code로 구현하고 AWS Lambda·SQS로 검증하기」 실습 저장소입니다.
Chapter 1~4 실습이 모두 들어 있습니다.

AWS 계정 없이 로컬에서 돌아갑니다. 모든 AWS 호출은 LocalStack 컨테이너로 갑니다.

## 규모와 검증 환경

- **1억 건은 설계 목표 규모**입니다. 처리량 예산은 `docs/reliability/concurrency-budget.md` 와 `scripts/measure/budget.py` 로 계산합니다.
- **로컬 실측은 수신자 10만 명 발송 접수까지**입니다(발송 완료가 아니라 접수 판정). 결과는 `evidence/first-e2e-run.md`.
- Planner 한 번의 처리 한계: 제한 시간 300초 안에 로컬 실측 기준 약 100만 명. 이보다 큰 캠페인은 수신자 파일을 나눠야 합니다(ADR-0001 결과 절, 이 저장소는 분할을 구현하지 않음).
- 상태 조회의 `sentCount` 는 원장 행 수라서 중복 발송까지 셉니다. 빠지거나 두 번 간 수신자는 `scripts/measure/count-results.sh` 의 키별 발송 수로 확인합니다(`docs/contracts/rest-contract-decisions.md` 알려진 한계).
- LocalStack 4.9 에서 확인되지 않아 실제 AWS 에서 확인할 항목:
  - 이벤트 소스 동시 실행 상한(`MaximumConcurrency`)이 실제로 동시 실행 수를 묶는지
  - fair queue(`MessageGroupId`)가 조용한 테넌트의 대기시간을 줄이는지
  - DLQ 재처리 메시지가 새 MessageId 로 들어오는지(`docs/operations/dlq-reprocess-runbook.md`)

## 준비물

| 도구 | 쓰는 곳 |
|---|---|
| JDK 21 | API·모의 발송사 실행과 빌드 |
| Docker | LocalStack 4.9, k6(`grafana/k6:2.2.0` 컨테이너, 1-7·2-10) |
| AWS CLI v2 | LocalStack 배포·조회 |
| Python 3 | `scripts/` 셸 스크립트 다수(`dispatch-campaign.sh`, `inspect-dlq.sh`, `vendor-stats.sh` 등)와 `budget.py`, `python3 -m json.tool` |
| Node.js | 2-3 명세 lint(`npx -y @redocly/cli@1 lint`) |
| (선택) SAM CLI | 템플릿 검토 |

명령 예시는 macOS 기준입니다. `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` 은 macOS 전용이라, Linux·Windows(WSL)에서는 `export JAVA_HOME=<JDK 21 설치 경로>` 로 직접 지정합니다(예: `/usr/lib/jvm/java-21-openjdk-amd64`).

## 구성

```
POST /campaigns ─▶ presigned URL 로 수신자 CSV 업로드(S3 lab-recipients)
POST /campaigns/{id}/dispatch ─▶ 202 ─▶ SQS notify-dispatch-planned ─▶ Planner Lambda
Planner: CSV 를 읽어 수신자마다 발송 메시지(payloadRef·idempotencyKey·traceId) ─▶ SQS notify-send-requested
Sender: payloadRef 확인 → 멱등 키 선점(조건부 쓰기·선점 만료) → 발송사 호출 → 결과 원장 send-results
        └ 3번 받고도 실패하면 ─▶ notify-send-requested-dlq
GET /campaigns/{id} ─▶ 결과 원장 기준 상태
```

| 경로 | 내용 |
|---|---|
| `api/` | Spring Boot 공개 API 3개, 동기 baseline(`/baseline/sync-dispatch`), traceId 필터 |
| `functions/planner/` | 수신자 파일 → 발송 메시지 펼치기 |
| `functions/sender/` | 발송 Lambda 와 멱등성 테스트 |
| `mock-vendor/` | 모의 발송사(지연·초당 한도·429·통계) |
| `template.yaml` | SAM 템플릿 |
| `openapi/`, `contracts/` | 공개 API 명세, 메시지 계약 |
| `perf/k6/` | 동기 baseline, 10만 명 접수 부하 스크립트 |
| `scripts/local/` | LocalStack 기동·배포·초기화·정리, API·모의 발송사 실행 |
| `scripts/replay/`, `scripts/data/` | 캠페인 발송·메시지 발행, 합성 수신자 생성 |
| `scripts/measure/` | 상태·큐 깊이·발송사 통계·재전달·테넌트 대기·예산 계산 |
| `scripts/ops/` | traceId 추적, DLQ 분류·전체/선별 재처리 |
| `scripts/aws/` | 실제 AWS 확인용(강사용) |
| `docs/` | 아키텍처·계약·신뢰성·운영 문서, ADR 3개, Claude Code 작업 기록 |
| `evidence/` | 강별 실행 증거 |

## 시작하기

```bash
source scripts/local/env.sh            # 더미 자격증명 + AWS_ENDPOINT_URL=localhost:4566
scripts/local/up.sh                    # LocalStack 4.9 (latest/stable 태그는 토큰을 요구해 바로 종료됨)
./gradlew buildZip :api:bootJar :mock-vendor:installDist   # JAVA_HOME 은 JDK 21
VENDOR_MODE=http scripts/local/deploy.sh conditional true
scripts/local/vendor.sh start 20 0 && scripts/local/api.sh start
scripts/replay/dispatch-campaign.sh tenant-a 100
```

정리: `scripts/local/teardown.sh` 다음에 `scripts/local/api.sh stop && scripts/local/vendor.sh stop`

## 테스트

```bash
./gradlew :api:test                                                                              # 공개 계약 3개(LocalStack 필요)
./gradlew :functions:sender:test --tests '*DuplicateSendTest' -Didempotency.mode=conditional     # 조건부 쓰기: 통과
./gradlew :functions:sender:test --tests '*DuplicateSendTest' -Didempotency.mode=check-then-put  # 비교용 구현: 동시 테스트 실패
./gradlew :functions:sender:test --tests '*IdempotencyRecoveryTest'                             # 선점 뒤 중단·응답 타임아웃 복구
```

LocalStack 이 떠 있어야 합니다. 테스트용 테이블을 만들고 끝나면 지웁니다.

## 3-5~3-9 단독 실습

3-5~3-9 는 발행 스크립트로 메시지를 직접 넣어 진행합니다.

```bash
./gradlew :functions:sender:buildZip
scripts/local/deploy.sh off false
scripts/local/seed-payloads.sh
```

모드 전환: `scripts/local/set-mode.sh <off|check-then-put|conditional> <report|throw>`
