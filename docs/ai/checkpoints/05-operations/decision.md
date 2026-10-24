# decision — 05-operations

판정 기준만 적는다. 실제 판정은 촬영 때 채운다.

| 강 | 대상 | accept | edit | reject | 판정 |
|---|---|---|---|---|---|
| 8-1 | traceId 전파 방식 | 메시지 본문 필드로 전파, 계약에 드러남 | 일부 로그 줄에 traceId 가 빠짐 | SQS 메시지 속성에만 싣거나 단계마다 새로 만듦 | 촬영 때 채움 |
| 8-4 | bootstrap 기동·종료 절차 | 종료와 빈 목록 확인이 있음 | 확인 명령이 없음 | 종료·삭제 단계가 없음 | 촬영 때 채움 |
| 8-4 | async SAM wiring·정리 절차 | 모든 리소스가 teardown 뒤 0개 | 스택 밖 리소스 누락 | 삭제 단계가 없거나 "필요하면 정리" | 촬영 때 채움 |

> teardown 이 빠진 제안은 부분 수정으로 넘기지 않고 reject 한다. 대체안은 `scripts/local/teardown.sh` 와 `docs/operations/teardown-checklist.md` 다.
