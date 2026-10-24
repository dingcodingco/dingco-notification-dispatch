# test-log — 02-contracts

환경: LocalStack 4.9 (로컬), `main` 브랜치. 실행 결과는 녹화 후 채운다.

| 강 | 명령 | 결과 |
|---|---|---|
| 3-3 | `npx @redocly/cli lint openapi/notification-api.yaml` | 녹화 후 채움 |
| 3-4 | `./gradlew :api:test` (스택이 떠 있는 셸에서) | 녹화 후 채움 |
| 3-5 | `NO_DISPATCH=1 scripts/replay/dispatch-campaign.sh tenant-a 100000` | 녹화 후 채움 |
| 3-5 | `docker run --rm -i --add-host=host.docker.internal:host-gateway -e CAMPAIGN=<campaignId> grafana/k6 run - < perf/k6/dispatch-accept.js` | 녹화 후 채움 |
| 3-5 | 같은 순서를 수신자 100명으로 | 녹화 후 채움 |
| 3-5 | 같은 캠페인에 dispatch 한 번 더 | 녹화 후 채움(409 이어야 함) |
