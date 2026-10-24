# 메시지 계약 결정 (v1)

기준: `contracts/messages/*.json` (JSON Schema 2020-12). 바꾸면 이 문서를 같이 고친다.

## 3종
| 계약 | 만드는 쪽 → 받는 쪽 | 전달 수단 |
|---|---|---|
| `NotificationDispatchPlanned` v1 | API(dispatch 접수) → Planner | SQS `notify-dispatch-planned`, 접수 1번에 1건 |
| `NotificationSendRequested` v1 | Planner → Sender | SQS `notify-send-requested`, 수신자 1명에 1건 |
| `NotificationSendResultRecorded` v1 | Sender(모의 발송사) → 원장 | **메시지 아님.** `send-results` 테이블의 행 |

## 최소 필드 7개 (`NotificationSendRequested`)
| 필드 | 있어야 하는 이유 |
|---|---|
| `campaignId` | 캠페인별 집계(`by-campaign`), 키 구성 |
| `tenantId` | 테넌트별 대기시간 측정, fair queue 그룹, 키 구성 |
| `recipientId` | 발송 대상. 합성 ID 만 쓴다 |
| `channel` | SMS·EMAIL·PUSH. 같은 수신자라도 채널이 다르면 다른 발송 |
| `payloadRef` | 본문 위치(`s3://` 로 시작) |
| `idempotencyKey` | 발송 결과를 1번으로 수렴시키는 기준 |
| `traceId` | 진입점부터 원장까지 추적 |

`type`·`version` 은 판별용으로 함께 싣는다. `additionalProperties: true` 이고 Sender(`SendRequest`)는 모르는 필드를 무시하므로, 필드 추가는 v1 안에서 할 수 있다. 필드 삭제·의미 변경은 v2 로 올린다.

## payloadRef: 본문 대신 S3 참조
- 같은 캠페인의 모든 메시지가 `s3://lab-payloads/{campaignId}/body.json` 하나를 가리킨다. 수신자 수만큼 본문을 복사하지 않는다.
- SQS 메시지 크기 제한에 본문 길이가 묶이지 않는다.
- Sender 는 선점 **전에** HeadObject 로 존재를 확인한다. 없으면 poison(`PoisonMessageException`)으로 보고, 키를 선점한 채 실패하지 않는다.

## idempotencyKey 구성
`tenantId:campaignId:recipientId:channel` (Planner 가 만든다)
- 업무 규칙 "같은 캠페인에서 같은 수신자에게 같은 채널로 두 번 보내지 않는다"에 맞춘 범위다.
- SQS `MessageId` 는 쓰지 않는다. 재발행·DLQ 선별 재처리로 같은 요청에 다른 MessageId 가 붙는다.
- Planner 가 같은 계획 메시지를 다시 처리해도 키가 같으므로 Sender 에서 흡수된다.
- 상세: `docs/reliability/idempotency-policy.md`.

## 결과를 메시지가 아니라 원장 행으로 남긴 결정
| 선택지 | 판단 |
|---|---|
| 결과 메시지를 큐로 발행 | 소비자가 없다. 결과 메시지도 at-least-once 라서 중복 집계 처리가 또 필요하다 |
| **원장 행으로 기록** (채택) | 행 수 = 발송사가 받은 발송 수. 중복 발송 판정, 상태 집계, 결과 조회가 한 테이블로 된다 |

스키마 파일은 행의 필드 정의로 남긴다(`sendId`, `campaignId`, `tenantId`, `recipientId`, `idempotencyKey`, `traceId`, `sentAt`). 결과를 다른 시스템에 알려야 하는 요구가 생기면 그때 메시지로 올린다.
