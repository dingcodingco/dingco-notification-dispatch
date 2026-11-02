# test-log — 00-bootstrap

환경: LocalStack 4.9 (로컬), `main` 브랜치. 실행 결과는 녹화 후 채운다.

| 강 | 명령 | 결과 |
|---|---|---|
| 1-4 | `./gradlew build -x test` | 녹화 후 채움 |
| 1-4 | `source scripts/local/env.sh && scripts/local/up.sh` | 녹화 후 채움 |
| 1-4 | `curl -s localhost:4566/_localstack/health` | 녹화 후 채움 |
| 1-4 | `scripts/local/down.sh` | 녹화 후 채움 |
| 1-4 | `docker ps --filter name=kan-lab` | 녹화 후 채움(빈 출력이어야 함) |
