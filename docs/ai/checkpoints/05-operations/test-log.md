# test-log — 05-operations

환경: LocalStack 4.9 (로컬), `main` 브랜치. 실행 결과는 녹화 후 채운다.

| 강 | 명령 | 결과 |
|---|---|---|
| 8-1 | `./gradlew :api:test :functions:sender:test` | 녹화 후 채움 |
| 8-2 | `scripts/replay/dispatch-campaign.sh tenant-a 3 trace-demo0001` | 녹화 후 채움 |
| 8-2 | `scripts/ops/trace-lookup.sh trace-demo0001` | 녹화 후 채움(네 구간) |
| 8-3 | `scripts/measure/campaign-status.sh <campaignId>` → `scripts/ops/inspect-dlq.sh` → `trace-lookup.sh` | 녹화 후 채움 |
| 8-4 | `scripts/local/teardown.sh` | 녹화 후 채움(남은 리소스 0개) |
| 8-4 | `scripts/local/api.sh stop && scripts/local/vendor.sh stop` | 녹화 후 채움 |
| 8-4 | `docker ps --filter name=kan-lab` | 녹화 후 채움(빈 출력) |
