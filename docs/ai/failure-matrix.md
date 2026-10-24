# 실패 유형 표

강의에서 재현하거나 판정한 실패를 한 표에 모은다. "막는 장치"가 있어도 남는 위험은 해당 정책 문서에 적는다.

| 실패 | 재현 방법 | 판정 기준 | 막는 장치 | 섹션 |
|---|---|---|---|---|
| 동기 timeout·부분 성공 | `perf/k6/sync-baseline.js` 로 `/baseline/sync-dispatch` 수신자 수를 늘림 | k6 `TIMEOUT/ERROR` 와 `/stats` `accepted` 증가가 같은 단계에서 함께 나옴 | 접수와 발송 분리(`dispatch` 는 계획 메시지 1건만) | 2 |
| 중복 전달(순차) | `scripts/replay/send-message.sh fixtures/messages/send-requested-rcp-0001.json 2` | 원장 행 수가 키마다 1 (`count-results.sh`) | `COMPLETED` 확인 | 6 |
| 중복 전달(동시) | `DuplicateSendTest` 동시 케이스(`CyclicBarrier`) | `conditional` 통과, `check-then-put` 은 `expected: <1> but was: <2>` | 조건부 쓰기 선점 | 6 |
| 선점 뒤 중단 | `IdempotencyRecoveryTest` | 만료 전 재전달은 발송 0, 만료 뒤 1 | 선점 만료(`leaseUntil`, 30초) | 6 |
| 응답 타임아웃(발송사는 처리) | `IdempotencyRecoveryTest` | 결과 조회 없음 2번, 있음 1번(`RECOVERED_FROM_VENDOR`) | 발송사 결과 조회(`by-idempotency-key`) | 6 |
| poison(payload 없음) | `scripts/replay/send-batch.sh` 로 `fixtures/messages/batch-*.json` 발행(복구용 payload 는 `fixtures/repair/` 에만 있음) | 해당 키만 수신 3회 후 DLQ, `inspect-dlq.sh` 판단 "payload 복구 후 재투입" | 선점 전 payload 확인, `maxReceiveCount: 3`, DLQ | 6 |
| 배치 전체 재시도 | `set-mode.sh conditional throw` 후 같은 배치 | `redelivery-summary.sh` "정상 메시지까지 재전달됨" | `ReportBatchItemFailures`(이벤트 소스 `FunctionResponseTypes`) | 6 |
| 발송사 429 | `vendor.sh start 20 <한도>` + `VENDOR_MODE=http` 로 제어 없이 발송 | `/stats` `throttled429` > 0, 수신 횟수 소모로 DLQ 유입 | `VENDOR_MAX_RPS`, `SenderMaxConcurrency`(실제 AWS) | 5 |
| 테넌트 쏠림 | 대량 테넌트와 소량 테넌트를 함께 `dispatch-campaign.sh` | `tenant-wait-report.sh` 의 조용한 테넌트 p50·p95 | Standard 큐 fair queue(`FAIR_QUEUE=true`, 효과는 실제 AWS 측정) | 7 |
| 추적 단절 | `trace-lookup.sh <traceId>` | API·Planner·Sender·원장 네 구간 중 빈 구간 | 메시지 본문 `traceId` 필드 전파 | 8 |
| 리소스 잔존 | `teardown.sh` 없이 종료하거나, 스택 밖 리소스를 추가 | 조회 결과가 0개가 아님 | `teardown.sh` + `teardown-checklist.md` | 8 |

## reject 한 AI 제안
| 제안 | 축 | 대체안 | 기록 |
|---|---|---|---|
| 멱등 키에 SQS MessageId 사용 | 계약·요구사항 오독 | `tenantId:campaignId:recipientId:channel` | `checkpoints/04-resilience/decision.md` |
| 공정성을 위해 FIFO 큐로 전환 | 요구사항 오독(순서 보장 도입) | Standard 큐 + `MessageGroupId=tenantId` | `docs/adr/0003-fair-queue-over-fifo.md` |
| teardown 단계가 없는 실행 절차 | 운영·비용 위험 | `teardown.sh` + 체크리스트 | `checkpoints/05-operations/decision.md`(촬영 때 채움) |

각 행의 실제 AI 출력 원문은 촬영 세션의 `request-context.md` 에서 채운다.
