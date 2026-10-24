# 동시 실행·처리량 예산 (Sender)

## 식
```
전체 발송 속도 ≈ 동시 실행 수 × 인스턴스당 속도  ≤  발송사 한도
```
| 항 | 이 저장소에서 정하는 곳 |
|---|---|
| 동시 실행 수 | 이벤트 소스 매핑 `ScalingConfig.MaximumConcurrency` ← 템플릿 파라미터 `SenderMaxConcurrency`(0=설정 안 함, 설정하면 2 이상) ← `SENDER_MAX_CONCURRENCY` |
| 인스턴스당 속도 | `VENDOR_MAX_RPS`(0=제한 없음). `HttpVendorClient.pace()` 가 실행 환경 하나에서 나가는 요청 간격을 `1/VENDOR_MAX_RPS` 초 이상으로 벌린다 |
| 발송사 한도 | mock-vendor `MOCK_VENDOR_RATE_LIMIT`(초 단위 고정 창, 넘으면 `429` + `Retry-After: 1`) ← `scripts/local/vendor.sh start <지연ms> <한도>` |

- `MaximumConcurrency` 는 **동시 실행 상한**이지 요청/초가 아니다. 요청/초는 mock-vendor `/stats` 의 `maxRequestsPerSecond` 로 따로 잰다.
- `VENDOR_MAX_RPS` 는 `VENDOR_MODE=http` 일 때만 쓰인다(`ledger` 모드는 발송사 HTTP 호출이 없다).

## 429 와 DLQ
- `429` 는 `VendorThrottledException` 이 되고, Sender 는 선점을 풀고 그 메시지를 실패 ID 로 보고한다. 호출 안에서 바로 재시도하지 않고 `Retry-After` 헤더도 읽지 않는다.
- 메시지는 가시성 제한 시간(20초) 뒤 다시 오고 수신 횟수가 1 늘어난다. `maxReceiveCount: 3` 에 닿으면 DLQ 로 간다.
- 따라서 429 가 계속되면 정상 메시지도 수신 횟수를 소모해 DLQ 로 밀려날 수 있다. 이 경우 원인은 poison 이 아니라 속도이므로, 예산을 고친 뒤 `scripts/ops/redrive-dlq.sh` 로 되돌린다.

## 계산기
```bash
scripts/measure/budget.py --vendor-rps <발송사 초당 한도> --per-instance <인스턴스당 초당 요청> [--recipients 100000000]
```
입력값으로 계산만 한다(측정값 아님). 출력: 동시 실행 상한(최대) = ⌊발송사 한도 ÷ 인스턴스당 속도⌋, 전체 발송 속도, 전체 소요 시간.

## 배포 예시
```bash
scripts/local/vendor.sh start 20 <한도>
VENDOR_MODE=http VENDOR_MAX_RPS=<인스턴스당> SENDER_MAX_CONCURRENCY=<상한> scripts/local/deploy.sh conditional true
scripts/measure/vendor-stats.sh      # accepted, throttled429, maxRequestsPerSecond
```

## 측정 기록
| 조건 | 동시 실행 상한 | 인스턴스당 속도 | 발송사 한도 | `maxRequestsPerSecond` | `throttled429` | DLQ |
|---|---|---|---|---|---|---|
| 제어 전 | Section 5 녹화로 채움 | | | | | |
| 제어 후 | Section 5 녹화로 채움 | | | | | |

## LocalStack 과 실제 AWS
- LocalStack 4.9 는 `MaximumConcurrency` 를 적용하지 않는다(`scripts/aws/README.md`). 로컬에서는 `VENDOR_MAX_RPS` 와 발송사 한도의 효과만 본다.
- 동시 실행 상한이 실제로 동시 실행 수를 제한하는지는 **강사 측 실제 AWS 측정값**으로 채운다.
