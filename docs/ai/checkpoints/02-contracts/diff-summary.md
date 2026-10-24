# diff-summary — 02-contracts

> 현재 저장소는 `main` 단일 브랜치다. 아래 표는 이 단계에서 다루는 파일을 강 기준으로 나눈 것이다.

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `openapi/notification-api.yaml`, `redocly.yaml` | 공개 경로 3개, `X-Trace-Id` 파라미터 | 응답 필드가 결정 문서와 같은지 |
| `CampaignController.java` | 201·202·200, 404·409 예외 매핑 | 경로가 3개뿐인지 |
| `CampaignService.java` | 생성(presign), 접수(HeadObject → 본문 S3 저장 → 조건부 QUEUED → 계획 메시지 1건), 조회(원장 `by-campaign` 집계) | 조건식 `#s = :draft`, 메시지가 1건인지 |
| `AwsConfig.java`, `LabProperties.java`, `application.yml` | LocalStack 엔드포인트, 업로드 URL 15분 | |
| `TraceIdFilter.java` | 진입점 traceId | 8-1 에서 다시 본다 |
| `contracts/messages/notification-dispatch-planned.json` | 계획 메시지 v1 | 구현이 만드는 JSON 과 필드가 같은지 |
| `CampaignApiTest.java` | 업로드 정보 5개, 미업로드 409, 업로드 후 202 QUEUED | |
| `perf/k6/dispatch-accept.js`, `scripts/replay/dispatch-campaign.sh`, `scripts/data/gen-recipients.sh` | 접수 판정, 합성 수신자 | 수신자가 합성 ID 인지 |
| `docs/contracts/rest-contract-decisions.md` | 3-1·3-2 산출물 | |
