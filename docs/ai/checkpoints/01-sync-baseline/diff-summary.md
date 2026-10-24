# diff-summary — 01-sync-baseline

> 현재 저장소는 `main` 단일 브랜치다. 아래 표는 이 단계에서 다루는 파일을 강 기준으로 나눈 것이다.

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `api/.../BaselineController.java` | 수신자 수만큼 발송사 차례 호출, `elapsedMs` 로그 | 요청 스레드에서 도는지, 병렬화로 "개선"하지 않았는지 |
| `api/src/main/resources/application.yml` | `lab.vendor-url` | |
| `mock-vendor/.../MockVendorServer.java` | 지연(`MOCK_VENDOR_LATENCY_MS`), 초당 한도(`MOCK_VENDOR_RATE_LIMIT`), `/stats` | `accepted` 를 세는 위치 |
| `scripts/local/vendor.sh`, `scripts/local/api.sh` | 기동·종료 | `stop` 이 있는지 |
| `perf/k6/sync-baseline.js` | `STEPS`, `TIMEOUT` 환경변수 | 판정·해석 문구가 섞이지 않았는지 |
| `docs/architecture/async-requirements.md` | 2-5 산출물 | 요구사항마다 근거 관찰이 있는지 |

결과는 `evidence/sync-timeout-baseline.md` 에 있다.
