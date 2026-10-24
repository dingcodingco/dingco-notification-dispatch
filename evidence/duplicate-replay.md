# duplicate 재현·검증 증거

환경: LocalStack 4.9 (로컬), 저장소 `main` 브랜치. 실AWS 미검증.
원본 출력: 교재 작업 폴더 `assets/raw/s6-*.txt`

## 재현 (6-1, `IdempotencyMode=off`)
- 같은 본문 2건 발행, MessageId 서로 다름
- 소비 로그 2줄 모두 `SENT`
- 원장: `2 tenant-a:cmp-0001:rcp-0001:SMS`

## 복구 (6-2, `IdempotencyRecoveryTest`)
| 경우 | 결과 |
|---|---|
| 선점 뒤 중단 | 즉시 재전달 `IN_PROGRESS_ELSEWHERE`(발송 0) → 선점 만료 2초 뒤 `SENT`(1) → 다음 재전달 `SKIPPED_DUPLICATE`(1) |
| 응답 타임아웃, 결과 조회 없음 | 재시도가 다시 보냄: 발송사 수신 2 |
| 응답 타임아웃, 결과 조회 있음 | `RECOVERED_FROM_VENDOR`: 발송사 수신 1 |

## 검증 (6-3)
| 테스트 | Claude Code 와 만든 구현(조건부 쓰기) | 비교용 구현(조회 후 저장) |
|---|---|---|
| 차례로 2건 | PASSED | PASSED |
| 동시에 2건(`CyclicBarrier`, 5라운드) | PASSED (늦은 쪽 `IN_PROGRESS_ELSEWHERE`) | FAILED `expected: <1> but was: <2>` |

LocalStack 재실행: 소비 로그 `SENT`+`SKIPPED_DUPLICATE`, 원장 1, 멱등 키 `COMPLETED`.

## 재처리 (6-5)
- 선별 재처리 2건(payload 복구한 키): 새 MessageId, 수신 1회, `SENT`
- 전체 재처리 4건(이미 발송된 키): 모두 `SKIPPED_DUPLICATE`
- 원장 9 → 11, 키마다 1
- 메시지 수와 발송 결과 수가 다른 이유: 전달은 여러 번 일어날 수 있고, 발송 여부는 멱등 키 상태로 정해진다.
