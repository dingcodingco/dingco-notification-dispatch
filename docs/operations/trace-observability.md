# traceId 추적

## 생성
- `TraceIdFilter`(Spring `OncePerRequestFilter`)가 모든 요청 진입점에서 정한다. 요청 헤더 `X-Trace-Id` 가 있으면 그대로 쓰고, 없으면 `trace-` + UUID 앞 8자리를 만든다.
- MDC `traceId` 에 넣어 로그 줄마다 `[traceId]` 로 찍히고, 응답 헤더 `X-Trace-Id` 로 돌려준다.
- 캠페인 생성 요청의 값은 `campaigns.traceId`, 발송 접수 요청의 값은 `campaigns.dispatchTraceId` 에 남는다. 아래 전파는 **발송 접수 요청의 traceId** 를 따른다.

## 전파
| 단계 | 어디에 싣는가 | 코드 |
|---|---|---|
| API 접수 | `NotificationDispatchPlanned.traceId` | `CampaignService.dispatch` |
| Planner | 펼친 `NotificationSendRequested` 전부에 같은 값 복사 | `PlannerHandler.plan` |
| Sender | 로그 `[sender-trace] ... traceId=`, 멱등 키 항목 `traceId` | `SenderHandler.trace`, `ConditionalWriteIdempotencyStore.claim` |
| 결과 원장 | `send-results` 행의 `traceId` | `MockVendorClient.send` |
| 발송사(http 모드) | 요청 본문 `traceId` | `HttpVendorClient.send` |

- SQS 메시지 속성이 아니라 메시지 본문 필드로 싣는다. 계약 파일(`contracts/messages/*.json`)에 드러나야 하기 때문이다.
- DLQ 재처리(`redrive-dlq.sh`, `redrive-selected.sh`)는 본문을 그대로 옮기므로 traceId 가 새로 만들어지지 않는다.

## 로그 위치
| 구간 | 위치 | 찾을 줄 |
|---|---|---|
| API | `api/build/api.log` | `[trace-xxxxxxxx] CampaignService - dispatch queued campaignId=...` |
| Planner | CloudWatch Logs `/aws/lambda/planner` | `[planner] campaignId=... recipients=... enqueued=... traceId=...` |
| Sender | CloudWatch Logs `/aws/lambda/sender` | `[sender-trace] messageId=... tenantId=... campaignId=... waitMs=... result=... traceId=...` |
| 원장 | DynamoDB `send-results` | `traceId` 속성 |

Sender 의 `[sender] messageId=... idempotencyKey=...` 줄에는 traceId 가 없다. 같은 `messageId` 로 `[sender-trace]` 줄과 잇는다.

## 조회
```bash
scripts/replay/dispatch-campaign.sh tenant-a 3 trace-demo0001     # X-Trace-Id 를 직접 지정
scripts/ops/trace-lookup.sh trace-demo0001
```
`trace-lookup.sh` 는 API 로그 → Planner 로그 → Sender 로그(각 최대 5줄) → 원장 행 수 순서로 출력한다. 네 구간이 모두 나와야 V6 통과로 본다. 한 구간이 비면 그 앞 경계에서 전파가 끊긴 것이다.

## 한계
- 로컬 조회는 `aws logs filter-log-events`(LocalStack)다. 강사 측 실제 AWS 확인은 CloudWatch Logs Insights 로 하고 출력 형식이 다를 수 있다.
- 원장 조회는 `scan` 이라 원장이 커지면 느려진다. traceId 인덱스는 두지 않았다.
