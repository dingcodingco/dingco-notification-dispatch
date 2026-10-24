# 컨텍스트 다이어그램

공개 API 는 접수만 하고, 수신자별 발송은 큐 뒤의 Lambda 가 맡는다. 이름은 `template.yaml`·`application.yml` 기준이다.

```
운영자(curl, k6, scripts/replay/dispatch-campaign.sh)
  │ POST /campaigns            ─▶ campaigns 테이블(DRAFT) + uploadUrl 발급
  │ PUT uploadUrl (CSV)        ─▶ S3 lab-recipients/recipients/{campaignId}.csv
  │ POST /campaigns/{id}/dispatch
  ▼
Spring Boot api (:8080) ── 본문 저장 ─▶ S3 lab-payloads/{campaignId}/body.json
  │  상태 DRAFT→QUEUED(조건부), 계획 메시지 1건
  ▼
SQS notify-dispatch-planned (가시성 330초)
  ▼ 배치 1
Planner Lambda ── CSV 읽기 ◀─ S3 lab-recipients
  │  수신자마다 NotificationSendRequested, 10건씩 발행
  │  campaigns: DISPATCHING, totalRecipients, enqueuedCount
  ▼
SQS notify-send-requested (가시성 20초) ──3번 받고도 실패──▶ notify-send-requested-dlq
  ▼ 배치 5, 윈도 5초
Sender Lambda ── payloadRef 확인(HeadObject) ─▶ S3 lab-payloads
  │  멱등 키 선점/완료 ─▶ DynamoDB idempotency-keys
  │  결과 조회(by-idempotency-key) ─▶ DynamoDB send-results
  ▼
모의 발송사: VENDOR_MODE=ledger(Lambda 안) | http(mock-vendor :8081)
  ▼
DynamoDB send-results(결과 원장, 행 1개 = 발송사가 받은 발송 1건)

GET /campaigns/{id} ◀── campaigns + send-results(by-campaign) 집계 + DLQ 근사 깊이
```

## 읽는 법
| 구간 | 기준 파일 |
|---|---|
| 공개 API 3개 | `openapi/notification-api.yaml`, `CampaignController.java` |
| 메시지 2종 | `contracts/messages/notification-dispatch-planned.json`, `notification-send-requested.json` |
| 결과 기록 | `contracts/messages/notification-send-result-recorded.json`(메시지가 아니라 원장 행) |
| 재시도·DLQ | `docs/reliability/retry-dlq-policy.md` |

- `/baseline/sync-dispatch` 는 Section 2 비교용 동기 경로이고 이 그림에 넣지 않는다.
- 같은 `traceId` 가 API 로그 → 계획 메시지 → 발송 메시지 → 원장 행으로 이어진다(`docs/operations/trace-observability.md`).
