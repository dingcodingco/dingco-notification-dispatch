# diff-summary — 00-bootstrap

> 현재 저장소는 `main` 단일 브랜치다. 아래 표는 이 단계에서 다루는 파일을 강 기준으로 나눈 것이고, 촬영 세션의 실제 diff 로 다시 채운다.

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `settings.gradle`, `gradle.properties`, `gradlew`, `gradle/wrapper/*` | 멀티모듈 4개 등록 | 모듈 이름이 책임 경계와 같은지 |
| `api/build.gradle`, `functions/*/build.gradle`, `mock-vendor/build.gradle` | Java 21 toolchain | Spring Boot 버전, Lambda 모듈에 불필요한 HTTP 클라이언트가 빠졌는지 |
| `api/.../ApiApplication.java` | 빈 Spring Boot 앱 | |
| `scripts/local/env.sh` | 더미 자격증명, `AWS_ENDPOINT_URL=http://localhost:4566` | 실제 AWS 로 나갈 경로가 없는지 |
| `scripts/local/up.sh`, `down.sh` | `localstack/localstack:4.9` 를 `--rm` 으로 기동·종료 | 이미지 태그가 `4.9` 인지, 종료 명령이 있는지 |
| `CLAUDE.md`, `README.md`, `.gitignore` | 작업 규칙, 시작 방법 | teardown·승인 우회 금지 규칙이 있는지 |
| `docs/architecture/*.md` | 1-2·1-3 산출물 | |

커리큘럼 초안의 `docker-compose.yml`·`docs/runbook/local-setup.md` 대신 이 저장소는 `scripts/local/up.sh`·`down.sh` 와 `README.md` 를 쓴다.
