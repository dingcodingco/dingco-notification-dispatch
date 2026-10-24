# test-log — 01-sync-baseline

환경: LocalStack 4.9 (로컬), `main` 브랜치. 실행 결과는 녹화 후 채운다.

| 강 | 명령 | 결과 |
|---|---|---|
| 2-1 | `scripts/local/vendor.sh start 20 0 && scripts/local/api.sh start` | 녹화 후 채움 |
| 2-1 | `curl -s -X POST localhost:8080/baseline/sync-dispatch -H 'Content-Type: application/json' -d '{"campaignId":"baseline-10","recipients":10}'` | 녹화 후 채움 |
| 2-4 | `docker run --rm -i --add-host=host.docker.internal:host-gateway grafana/k6 run - < perf/k6/sync-baseline.js` | 녹화 후 채움 |
| 2-4 | `scripts/measure/vendor-stats.sh` | 녹화 후 채움 |
| 2-4 | `-e STEPS=... -e TIMEOUT=...` 로 단계·timeout 변경 실행 | 녹화 후 채움 |
