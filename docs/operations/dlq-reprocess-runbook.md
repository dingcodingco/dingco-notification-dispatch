# DLQ 재처리 runbook

전제: `source scripts/local/env.sh`, LocalStack 과 스택이 떠 있다.

## 1. 원인 확인
`scripts/ops/inspect-dlq.sh` — 메시지를 지우지 않고 키마다 원장 발송 수·payload 존재·판단을 본다.

## 2. 재처리 방식 고르기
| 방식 | 명령 | 쓰는 경우 |
|---|---|---|
| 전체 재처리 | `scripts/ops/redrive-dlq.sh` (SQS 메시지 이동 작업) | DLQ 의 모든 메시지가 같은 원인이고 그 원인이 고쳐졌을 때. 이미 보낸 메시지가 섞여 있어도 멱등 처리로 건너뛴다 |
| 선별 재처리 | `scripts/ops/redrive-selected.sh <키...>` (받기 → 본 큐로 새로 보내기 → DLQ 에서 삭제) | 원인이 여러 가지라 고친 것만 먼저 보내야 할 때, 아직 고치지 못한 메시지를 DLQ 에 남겨야 할 때 |
| 재처리하지 않음 | 폐기 사유를 기록하고 삭제 | 업무상 더 이상 보낼 필요가 없을 때(캠페인 취소, 만료된 알림) |

- SQS 메시지 이동 작업은 DLQ 전체를 옮긴다. 골라 옮기거나 내용을 고치는 기능은 없다(AWS 문서: "doesn't support filtering and modifying messages while redriving").
- 원인을 고치지 않은 메시지를 되돌리면 같은 이유로 다시 실패해 DLQ 로 돌아온다.

## 3. 재처리 전 기록
`scripts/measure/count-results.sh | tail -1` — 원장 전체 행 수를 적어 둔다.

## 4. 재처리
`T0=$(date +%s)` 로 시각을 남기고 재처리한 뒤 `scripts/measure/wait-drain.sh`, `scripts/measure/sender-logs.sh --since $T0`.
이미 발송된 키는 `SKIPPED_DUPLICATE`, 복구한 키만 `SENT` 여야 한다.

## 5. 원장·큐 확인
`scripts/measure/count-results.sh`, `scripts/measure/queue-depth.sh` — 키마다 1, 늘어난 행 수 = 새로 보낸 건수, 큐 2개 0.

## 6. 정리
`scripts/local/teardown.sh` — 남은 큐·테이블·함수·버킷·스택 0개.

## LocalStack 과 실AWS 의 차이
| 항목 | LocalStack 4.9 관찰 | 실AWS |
|---|---|---|
| 메시지 이동 작업 뒤 MessageId | 원래 값 유지 | AWS 문서: 재처리 메시지는 새 MessageId 와 enqueueTime 을 받는 새 메시지 |
| 메시지 이동 작업 뒤 수신 횟수 | 초기화되지 않고 누적돼 넘어옴(DLQ 를 훑은 횟수만큼 더해짐. 실행마다 7·8 등으로 다름) | 강사 측 실AWS 확인 결과로 채운다(`scripts/aws/s6-redrive-check.sh`) |
| 선별 재처리(직접 다시 보내기) | 새 MessageId, 수신 1회 | 같다(새로 보낸 메시지) |
| `start-message-move-task` 목적지 | 목적지를 명시해야 동작 | 생략하면 원래 큐로 간다 |

## 기록칸
| 날짜 | DLQ 건수 | 원인 | 복구 | 재처리 방식 | 원장 전/후 | 폐기(사유) |
|---|---|---|---|---|---|---|
| (파일럿) | 6 | payload 누락 2, 끔 상태에서 밀려난 정상 4 | payload 2개 업로드 | 선별 2건 → 전체 4건 | 9 / 11 | 없음 |
