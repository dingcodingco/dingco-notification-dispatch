# diff-summary — Section 6 (main)

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `ConditionalWriteIdempotencyStore.java` | 조건부 PutItem 선점(없거나 만료된 IN_PROGRESS 일 때만), 상태 IN_PROGRESS→COMPLETED, 선점 만료 30초, TTL 7일, owner 일치 시에만 완료·해제 | 조건식, 남의 선점을 지우지 않는지 |
| `CheckThenPutIdempotencyStore.java` | 조회 후 저장. 비교용으로 준비한 구현 | GetItem 과 PutItem 사이의 틈 |
| `SenderService.java` | 검증 → payload 확인 → 선점 → 발송사 결과 조회 → 발송 → 완료, 실패 시 선점 해제 | 결과 조회가 발송보다 앞인지 |
| `MockVendorClient.java`, `template.yaml` | 원장 보조 인덱스 `by-idempotency-key` 로 결과 조회 | |
| `IdempotencyRecoveryTest.java` | 선점 뒤 중단, 응답 타임아웃(조회 없음/있음) | |
| `scripts/measure/redelivery-summary.sh`, `scripts/ops/redrive-selected.sh` | 재전달 판정, 선별 재처리 | |
| `S3PayloadResolver.java` | payloadRef 객체 없으면 poison | 404 외 S3 오류는 poison 으로 바꾸지 않는지 |
| `SenderHandler.java` | `SQSBatchResponse` 로 실패 ID 반환, 끔일 때 예외 | 성공 메시지 ID 가 실패 목록에 섞이지 않는지 |
| `template.yaml` | 파라미터 `ReportBatchItemFailures` → `FunctionResponseTypes` (끔은 `[]`) | 함수 환경변수와 이벤트 소스가 같은 파라미터를 보는지 |
| `DuplicateSendTest.java` | 순차 1건 + 동시(`CyclicBarrier`) 5라운드 | 발송 수를 세는 위치 |
| `scripts/ops/*` | DLQ 분류·재투입 | 재투입 목적지를 본 큐로 명시하는지 |
