# test-log — 03-async-pipeline

환경: LocalStack 4.9 (로컬), `main` 브랜치. 실행 결과는 녹화 후 채운다. `MaximumConcurrency` 효과는 LocalStack 에서 확인할 수 없다(강사 측 실제 AWS 측정).

| 강 | 명령 | 결과 |
|---|---|---|
| 4-1 | `./gradlew buildZip :api:bootJar :mock-vendor:installDist` | 녹화 후 채움 |
| 4-1 | `scripts/local/deploy.sh conditional true` | 녹화 후 채움 |
| 4-5 | `scripts/local/api.sh start && scripts/replay/dispatch-campaign.sh tenant-a 1000` | 녹화 후 채움 |
| 4-5 | `scripts/measure/wait-campaign.sh <campaignId>`, `scripts/measure/queue-depth.sh` | 녹화 후 채움 |
| 5-2 | `scripts/local/vendor.sh start 20 <한도>` + `VENDOR_MODE=http scripts/local/deploy.sh conditional true` | 녹화 후 채움 |
| 5-2 | `scripts/measure/vendor-stats.sh` | 녹화 후 채움 |
| 5-3 | `scripts/measure/budget.py --vendor-rps <한도> --per-instance <인스턴스당>` | 녹화 후 채움 |
| 5-4 | `VENDOR_MODE=http VENDOR_MAX_RPS=<값> scripts/local/deploy.sh conditional true` 후 같은 캠페인 | 녹화 후 채움 |
| 5-4 | `scripts/local/teardown.sh` | 녹화 후 채움 |
