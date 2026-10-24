# test-log — Section 6

환경: LocalStack 4.9 (로컬), `main` 브랜치, 2026-10-21 녹화 세션. 실AWS 미검증.

| 강 | 명령 | 결과 |
|---|---|---|
| 6-1 | `send-message.sh ... 2` (off) | 원장 2 |
| 6-2 | 조건부 `put-item` 2회 | 1회 저장됨 / 2회 `ConditionalCheckFailedException` |
| 6-2 | `./gradlew :functions:sender:test --tests '*IdempotencyRecoveryTest'` | 3 tests PASSED (중단 복구 1회, 타임아웃 조회 없음 2회/있음 1회) |
| 6-3 | `--tests '*DuplicateSendTest' -Didempotency.mode=conditional` | 2 tests PASSED |
| 6-3 | `--tests '*DuplicateSendTest' -Didempotency.mode=check-then-put` | 순차 PASSED / 동시 FAILED `expected: <1> but was: <2>` |
| 6-3 | `send-message.sh ... 2` (conditional) | 로그 `SENT`+`SKIPPED_DUPLICATE`, 원장 1, 키 `COMPLETED` |
| 6-4 | `send-batch.sh batch-a` (끔) + `redelivery-summary.sh` | 정상 4건 모두 재수신 → "정상 메시지까지 재전달됨" |
| 6-4 | `send-batch.sh batch-b` (켬) + `redelivery-summary.sh` | 정상 재수신 0건 → "실패한 메시지만 재전달됨" |
| 6-5 | 복구 2건 → `redrive-selected.sh` → `redrive-dlq.sh` | 선별 2건 SENT(새 MessageId), 전체 4건 SKIPPED, 원장 9 → 11 |
| 6-5 | `teardown.sh` | 남은 리소스 0개 |
