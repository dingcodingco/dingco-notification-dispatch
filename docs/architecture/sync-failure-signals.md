# 동기 발송이 대량에서 무너지는 신호

대상 코드: `BaselineController.java` (`POST /baseline/sync-dispatch`, 공개 계약 밖). 요청 스레드 안에서 수신자 수만큼 모의 발송사 `/send` 를 차례로 부른다.

용어: **접수**는 서버가 발송 요청을 받아들였다는 응답이고, **발송 완료**는 발송사가 수신자별 요청을 모두 받은 상태다. 동기 경로는 둘이 같은 응답에 묶여 있다.

## 신호 4가지
| 신호 | 생기는 순서 | Section 2 에서 확인할 것 | 측정 결과 |
|---|---|---|---|
| 응답 시간 선형 증가 | 수신자 1명마다 발송사 지연(`MOCK_VENDOR_LATENCY_MS`)이 더해진다. 응답 시간 ≈ 수신자 수 × 호출 1번 시간 | `perf/k6/sync-baseline.js` 의 `STEPS` 단계별 `duration_ms`, API 로그 `elapsedMs` | Section 2 측정으로 채움 |
| 클라이언트 timeout | 응답 시간이 클라이언트 timeout(`TIMEOUT`, 기본 `10s`)을 넘으면 호출자는 실패를 받는다 | k6 출력의 `TIMEOUT/ERROR` 단계 | Section 2 측정으로 채움 |
| 부분 성공 | 호출자가 끊어도 서버 루프는 계속 돈다. 호출자는 실패를 받았는데 발송사는 일부 또는 전부를 받았다 | 같은 실행에서 k6 실패 단계와 `/stats` 의 `accepted` 증가분 비교 | Section 2 측정으로 채움 |
| 재시도 시 중복 | 호출자가 실패로 보고 다시 부르면 이미 받은 수신자에게 다시 보낸다. 동기 경로에는 멱등 키가 없다 | 같은 단계를 두 번 부른 뒤 `accepted` 가 수신자 수의 몇 배인지 | Section 2 측정으로 채움 |

요청 스레드 점유도 같은 원인에서 나온다. 요청 하나가 끝날 때까지 서블릿 스레드 하나를 쥐고 있다.

## 측정 준비
```bash
scripts/local/vendor.sh start 20 0        # 지연 20ms, 한도 없음
scripts/local/api.sh start
docker run --rm -i --add-host=host.docker.internal:host-gateway grafana/k6 run - < perf/k6/sync-baseline.js
scripts/measure/vendor-stats.sh
```

- 위 지연 20ms 는 `vendor.sh` 의 기본값이고 실제 발송사 지연이 아니다.
- 결과는 `evidence/sync-timeout-baseline.md` 에 남기고, 해석은 사람이 쓴다.
