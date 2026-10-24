# 첫 E2E 실행과 10만 명 접수 증거

환경: LocalStack 4.9 (로컬), 저장소 `main` 브랜치. 실AWS 미검증.
원본 출력: 교재 작업 폴더 `assets/raw/s4-*.txt`

## 배포된 리소스 (4-1)
- 큐: `notify-dispatch-planned`, `notify-send-requested`, `notify-send-requested-dlq`
- 테이블: `campaigns`, `idempotency-keys`, `send-results`
- 함수: `planner`(java21, 300초), `sender`(java21, 15초)
- 버킷: `lab-artifacts`, `lab-payloads`, `lab-recipients`

## 펼친 메시지 (4-3, Sender 트리거 끔)
- 수신자 3명 → `enqueued=3`, 큐 깊이 3
- 메시지에 본문 없이 `payloadRef: s3://lab-payloads/{campaignId}/body.json`, `idempotencyKey: {tenant}:{campaign}:{recipient}:{channel}`, `traceId`

## 처음부터 끝까지 (4-5, 수신자 100명, 발송사 HTTP)
- 발송 접수 202 (0.05초)
- `완료: 100/100 — 약 38초`, 상태 `COMPLETED`
- 발송사 `accepted=100`, `throttled429=0`

## V1: 수신자 10만 명 접수 (Sender 트리거 끔)
- k6 dispatch 요청: `status=202`, 43ms
- Planner: `recipients=100000 enqueued=100000 elapsedMs=29537`
- 큐 깊이 100000
- 판정 범위: **접수**. 발송 완료 시간은 측정 대상이 아니다.
