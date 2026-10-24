# 재시도·DLQ 정책 (Sender)

## 설정값
| 항목 | 값 | 이유 |
|---|---|---|
| 배치 크기 / 배치 윈도 | 5 / 5초 | 배치 단위 실패를 보기 위한 실습값. 실제 묶음은 실행마다 다르다 |
| 함수 제한 시간 | 15초 | |
| 본 큐 가시성 제한 시간 | 20초 | 함수 제한 시간보다 길게 |
| `maxReceiveCount` | 3 | 3번 받고도 실패하면 DLQ |
| 부분 배치 실패 보고 | 켬 (`ReportBatchItemFailures`) | 아래 판정 |
| 선점 만료 | 30초 | `idempotency-policy.md` |

## failure class
| 분류 | 정의 | Sender 동작 | 결과 |
|---|---|---|---|
| duplicate | 이미 처리된 요청이 다시 옴 | `SKIPPED_DUPLICATE`, 성공 처리 | 큐에서 삭제 |
| 선점 중 | 다른 처리가 같은 키를 선점 중 | 실패 ID 로 보고 | 나중에 다시 받음 |
| poison | 몇 번을 받아도 같은 이유로 실패(이 실습: payloadRef 의 S3 객체 없음) | 실패 ID 로 보고 | 3회 뒤 DLQ |
| 일시적 실패(429 등) | 기다리면 성공할 수 있음 | Section 5 재시도 경로 | poison 과 같은 분기로 두지 않는다 |

## 부분 배치 실패 보고 — 판정 기준
판정은 **DLQ 건수가 아니라 재시도 동작**으로 한다. 배치가 어떻게 묶이는지는 실행마다 달라서 DLQ 건수는 달라질 수 있다.

`scripts/measure/redelivery-summary.sh <검색어>` 로 키별 최대 수신 횟수와 결과를 본다.
| 설정 | 기대 판정 |
|---|---|
| 끔 | 정상 메시지도 수신 2회 이상(`SKIPPED_DUPLICATE`) → "정상 메시지까지 재전달됨" |
| 켬 | 정상 메시지는 수신 1회, 실패 메시지만 3회 → "실패한 메시지만 재전달됨" |

- 함수가 `batchItemFailures` 를 돌려줘도 이벤트 소스의 `FunctionResponseTypes` 가 비어 있으면 반환값은 무시된다. 템플릿 파라미터 하나로 함수 환경변수와 이벤트 소스를 함께 바꾼다.
- LocalStack 에서는 `FunctionResponseTypes` 를 속성 제거로 끄면 반영되지 않았다. 빈 목록으로 끈다.

재처리는 `docs/operations/dlq-reprocess-runbook.md` 를 따른다.
