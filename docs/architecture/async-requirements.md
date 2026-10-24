# 비동기 전환 요구사항

근거 관찰은 `sync-failure-signals.md` 의 신호다. 각 요구사항은 검증 태그(V1~V7)로 판정한다.

| ID | 근거 관찰 | 요구사항 | 구현 위치 | 검증 |
|---|---|---|---|---|
| R1 | 응답 시간 선형 증가, 클라이언트 timeout | 접수와 발송을 나눈다. 발송 시작 응답은 접수(`202`, `QUEUED`)만 하고 수신자 수와 관계없이 바로 온다 | `CampaignService.dispatch`(계획 메시지 1건만 발행) | V1 `perf/k6/dispatch-accept.js` |
| R2 | 부분 성공 | 건 단위로 실패를 나누고 진행을 조회할 수 있다 | 발송 큐 메시지 1건 = 수신자 1명, `GET /campaigns/{id}` | V1 |
| R3 | 재시도 시 중복 | 같은 발송 요청이 여러 번 처리돼도 발송 결과가 1번으로 수렴하는 키가 있다 | `idempotencyKey`, `ConditionalWriteIdempotencyStore` | V2 |
| R4 | 부분 성공 | 실패한 메시지만 다시 받고, 계속 실패하면 격리한 뒤 원인을 고쳐 재투입한다 | `ReportBatchItemFailures`, `maxReceiveCount: 3`, DLQ, `scripts/ops/` | V4 |
| R5 | 발송사 지연에 묶인 구조 | 발송사 호출 속도를 우리 쪽에서 따로 제어한다 | `VENDOR_MAX_RPS`, `SenderMaxConcurrency` | V3 |
| R6 | (Section 7 관찰 필요) | 한 테넌트가 몰려도 다른 테넌트의 대기시간을 측정하고 줄인다 | `FAIR_QUEUE`(MessageGroupId=tenantId) | V5 |
| R7 | 실행이 여러 단계로 나뉨 | 한 요청을 진입부터 결과 기록까지 같은 `traceId` 로 따라간다 | `TraceIdFilter`, 메시지 `traceId` 필드 | V6 |
| R8 | 리소스가 늘어남 | 만든 리소스를 한 명령으로 지우고, 남은 리소스 0개를 조회로 확인한다 | `scripts/local/teardown.sh` | V7 |

## 범위 밖
Spring Security, 실벤더 연동, Kafka·EventBridge·Step Functions 비교, 멀티 리전, 운영 대시보드.

## 이 문서가 말하지 않는 것
- 메시지 전달은 at-least-once 다. 중복 전달 자체를 없애는 요구사항은 두지 않고, 발송 결과를 키마다 1번으로 수렴시킨다.
- 접수 판정(V1)은 발송 완료 건수 판정이 아니다. 완료 건수는 결과 원장 행 수로 따로 본다.
