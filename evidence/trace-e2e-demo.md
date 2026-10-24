# traceId E2E 추적 증거

환경: LocalStack 4.9 (로컬), 저장소 `main` 브랜치. 실AWS 미검증.
원본 출력: 교재 작업 폴더 `assets/raw/s8-*.txt`

## 진입점 (8-1)
- 헤더 없이 `POST /campaigns` → 응답 `X-Trace-Id: trace-6d0e2bd3` (필터가 새로 만듦)
- `X-Trace-Id: trace-lab-0001` 을 주고 요청 → 응답에 같은 값
- 같은 traceId 로 수신자 20명 발송 → `완료: 20/20 — 약 21초`

## 한 번에 따라가기 (8-2, `scripts/ops/trace-lookup.sh trace-lab-0001`)
| 구간 | 남은 흔적 |
|---|---|
| API | `[trace-lab-0001] ... dispatch queued campaignId=cmp-4c896ff1` |
| Planner | `[planner] ... recipients=20 enqueued=20 ... traceId=trace-lab-0001` |
| Sender | `[sender-trace] ... result=SENT traceId=trace-lab-0001` (waitMs 포함) |
| 결과 원장 | `send-results` 에서 traceId 로 20건 |

## 장애 추적 (8-3, `trace-incident-01`)
- 조건: 발송사 한도 초당 1건, 수신자 30명
- 120초 뒤 메인 큐 0, DLQ 9 / 발송사 accepted 21, throttled429 47
- `inspect-dlq.sh`: DLQ 9건 모두 원장 0·payload 있음 → 재투입 대상
- traceId 로 원장 21건. 30 = 발송 21 + DLQ 9
