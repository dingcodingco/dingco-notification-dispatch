# Notification Dispatch Platform Lab — Claude Code 작업 규칙

## 무엇을 만드는가
대량 알림 발송 시스템. Spring Boot 가 공개 API 3개(캠페인 생성·발송 접수·상태 조회)를 맡고, SQS·Lambda 가 발송을 맡는다.
- `api/` Spring Boot 3.5, Java 21 — 공개 API, 동기 baseline(`/baseline/sync-dispatch`, 공개 계약 밖)
- `functions/planner/` 수신자 파일 → 수신자별 발송 메시지(fan-out)
- `functions/sender/` 발송 메시지 소비 → 멱등 처리 → 발송사 호출 → 결과 원장
- `mock-vendor/` 모의 발송사(지연·429·통계)
- `template.yaml` SAM. 로컬은 LocalStack `4.9` 에 배포한다

## 지켜야 할 것
- 공개 계약은 `openapi/notification-api.yaml`, 메시지 계약은 `contracts/messages/*.json` 이 기준이다. 바꾸면 결정 문서(`docs/contracts/`)를 같이 고친다.
- 메시지 v1 최소 필드: campaignId, tenantId, recipientId, channel, payloadRef, idempotencyKey, traceId. 본문은 메시지에 싣지 않고 payloadRef 로 참조한다.
- 발송은 at-least-once 다. 같은 메시지가 여러 번 와도 발송이 1번으로 수렴하게 만든다(조건부 쓰기, 상태, 선점 만료, 결과 조회).
- 모든 리소스는 `scripts/local/teardown.sh` 로 지울 수 있어야 한다. teardown 을 빠뜨린 변경은 받지 않는다.
- 실제 AWS 자격증명·회사 데이터·실제 연락처를 쓰지 않는다. 수신자는 `scripts/data/gen-recipients.sh` 의 합성 ID 만 쓴다.
- `--dangerously-skip-permissions` 같은 승인 우회 실행을 쓰지 않는다.

## 자주 쓰는 명령
```bash
source scripts/local/env.sh && scripts/local/up.sh
./gradlew buildZip :api:bootJar :mock-vendor:installDist
scripts/local/deploy.sh conditional true          # VENDOR_MODE=http 등은 환경변수
scripts/local/vendor.sh start 20 0 && scripts/local/api.sh start
./gradlew :api:test :functions:sender:test
scripts/local/teardown.sh
```

## 작업 기록
섹션마다 `docs/ai/checkpoints/<단계>/` 에 요청·컨텍스트, diff 요약, 테스트 로그, accept/edit/reject 판단을 남긴다.
