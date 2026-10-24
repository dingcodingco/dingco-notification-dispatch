# test-log — main

환경: LocalStack 4.9 (로컬), `main` 브랜치. 실행 결과는 녹화 후 채운다.

| 강 | 명령 | 결과 |
|---|---|---|
| 9-1 | V1~V7 증거 경로마다 `ls <경로>` | 녹화 후 채움 |
| 9-3 | `./gradlew :api:test :functions:sender:test` | 녹화 후 채움 |
| 9-3 | `./gradlew :functions:sender:test --tests '*DuplicateSendTest' -Didempotency.mode=conditional` | 녹화 후 채움 |
| 9-3 | `npx @redocly/cli lint openapi/notification-api.yaml` | 녹화 후 채움 |
| 9-3 | `verification-checklist.md` #10 기준으로 `docs`·`README.md` 의 보장 표현 검색 | 녹화 후 채움(빈 출력) |
| 9-3 | `scripts/local/teardown.sh` | 녹화 후 채움 |
