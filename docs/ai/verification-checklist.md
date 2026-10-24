# Claude Code 출력 수용 전 확인 목록

Claude Code 가 낸 코드·설정·문서를 받기 전에 사람이 확인한다. 하나라도 걸리면 accept 하지 않고 edit 또는 reject 로 판정해 `docs/ai/checkpoints/<단계>/decision.md` 에 남긴다.

| # | 확인 | 방법 | 걸리면 |
|---|---|---|---|
| 1 | 테스트를 실제로 돌렸고 통과했다 | `./gradlew :api:test :functions:sender:test` 출력을 직접 본다. "통과할 것" 같은 설명은 증거로 치지 않는다 | edit |
| 2 | 공개 계약과 구현이 일치한다 | `openapi/notification-api.yaml` 의 경로 3개·응답 필드와 `CampaignController`/`CampaignService` 비교, `npx @redocly/cli lint` | reject(경로 추가·필드 변경을 결정 문서 없이 함) |
| 3 | 메시지 계약을 지킨다 | v1 최소 필드 7개가 모두 있고 본문은 `payloadRef` 로만 참조한다. 계약을 바꿨으면 `docs/contracts/` 도 바뀌었다 | reject |
| 4 | 멱등 키가 업무 키다 | `idempotencyKey` 가 SQS `MessageId` 가 아니다. 선점이 조건부 쓰기 한 번이다(조회 후 저장 아님) | reject |
| 5 | 동시 경우까지 검증했다 | `DuplicateSendTest` 의 동시 케이스가 `-Didempotency.mode=conditional` 로 통과한다 | edit |
| 6 | 실패를 삼키지 않는다 | Sender 가 예외를 로그만 찍고 성공 처리하지 않는다. 실패 메시지 ID 만 `batchItemFailures` 에 들어간다 | reject |
| 7 | teardown 으로 지워진다 | 새 리소스가 `template.yaml` 안에 있고 `scripts/local/teardown.sh` 실행 뒤 조회가 0개다 | reject |
| 8 | 비밀값·실데이터가 없다 | 실제 AWS 자격증명·회사 데이터·실제 연락처 없음. 수신자는 `scripts/data/gen-recipients.sh` 합성 ID | reject |
| 9 | 승인 우회가 없다 | 실행 예시에 권한 승인을 건너뛰는 실행 옵션이 없다(`CLAUDE.md` 규칙) | reject |
| 10 | 보장 표현·지어낸 수치가 없다 | 보장 문구, 측정하지 않은 처리량·비용 숫자가 없다. 멱등 결과는 "1번으로 수렴"으로 쓴다 | edit |
| 11 | LocalStack 과 실제 AWS 를 섞지 않았다 | LocalStack 4.9 미지원 동작(`MaximumConcurrency`, fair queue 스케줄링)을 로컬 결과로 주장하지 않는다 | edit |
| 12 | 설정 변경은 템플릿 파라미터로 한다 | 함수 설정을 콘솔·CLI 로 직접 고치지 않고 `deploy.sh`/`set-mode.sh` 로 다시 배포한다 | edit |

## 판정 기록 형식
| 강 | 대상 | 판정 | 걸린 항목(#) | 근거 |
|---|---|---|---|---|
| (예) 6-2 | 멱등 키에 MessageId 사용 | reject | 4 | 재처리 중복을 못 막는다 |
