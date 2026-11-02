# diff-summary — 03-async-pipeline

> 현재 저장소는 `main` 단일 브랜치다. 아래 표는 이 단계에서 다루는 파일을 강 기준으로 나눈 것이다.

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `template.yaml` | 버킷 2, 테이블 3(GSI `by-idempotency-key`·`by-campaign`), 큐 3, 함수 2, 파라미터 | 가시성 제한 시간 > 함수 제한 시간, `maxReceiveCount: 3` |
| `contracts/messages/notification-send-requested.json`, `notification-send-result-recorded.json` | 메시지 v1, 원장 행 정의 | 최소 필드 7개 |
| `functions/planner/.../PlannerHandler.java` | CSV 스트리밍 읽기, 10건 배치 발행, 실패 항목 1회 재발행, `FAIR_QUEUE` | 발송사 호출이 없는지, 키 구성 |
| `functions/sender/.../SenderHandler.java`, `SenderService.java`, `SendRequest.java`, `Clients.java` | 소비·처리 골격 | 예외를 삼키지 않는지 |
| `MockVendorClient.java`, `HttpVendorClient.java`, `VendorClient.java`, `VendorThrottledException.java`, `VendorTimeoutException.java` | ledger/http 모드, `VENDOR_MAX_RPS` | 429 를 일시적 실패로 분리하는지 |
| `functions/*/build.gradle` | `buildZip` | |
| `scripts/local/deploy.sh`, `teardown.sh`, `seed-payloads.sh`, `reset.sh`, `set-mode.sh` | 배포·정리·초기화 | teardown 이 모든 리소스를 지우는지 |
| `scripts/measure/{wait-campaign,campaign-status,queue-depth,vendor-stats}.sh`, `budget.py` | 진행·발송사 통계·예산 계산 | 근사 메트릭으로 건수를 주장하지 않는지 |
| `docs/contracts/message-contract-decisions.md`, `docs/reliability/concurrency-budget.md` | 결정 문서 | |

결과는 `evidence/first-e2e-run.md`, `evidence/vendor-429-backpressure.md` 에 있다.
