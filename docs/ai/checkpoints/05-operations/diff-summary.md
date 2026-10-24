# diff-summary — 05-operations

> 현재 저장소는 `main` 단일 브랜치다. 아래 표는 이 단계에서 다루는 파일을 강 기준으로 나눈 것이다.

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `api/.../TraceIdFilter.java`, `application.yml`(로그 패턴 `[%X{traceId}]`, `build/api.log`) | 진입점 traceId, MDC, 응답 헤더 | 요청 헤더 값을 그대로 쓰는지 |
| `CampaignService.java` | 계획 메시지 `traceId`, `dispatchTraceId` 저장 | |
| `PlannerHandler.java` | 펼친 메시지마다 같은 `traceId`, 처리 로그에 `traceId` | 새로 만들지 않는지 |
| `SenderHandler.java` | `[sender-trace]` 줄(waitMs·traceId) | |
| `MockVendorClient.java` | 원장 행 `traceId` | |
| `scripts/ops/trace-lookup.sh` | API 로그 → Planner → Sender → 원장 | 네 구간을 모두 찾는지 |
| `scripts/local/teardown.sh`, `down.sh` | 버킷 비우기 → 스택 삭제 → 0개 확인 → 컨테이너 종료 | 로그 그룹은 세지 않음(체크리스트에 적음) |
| `docs/operations/trace-observability.md`, `teardown-checklist.md` | 운영 문서 | |

추적 결과는 `evidence/trace-e2e-demo.md` 에 있다. 8-3 incident note 는 학습자가 이 결과로 작성한다.
