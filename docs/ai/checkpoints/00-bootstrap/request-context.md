# request-context — 00-bootstrap (Section 1)

> 촬영 전 초안: 아래 요청은 촬영 때 칸이 Claude Code 에 실제로 입력하고, 그 세션의 출력으로 diff-summary·decision 을 채운다. 실제 AI 출력은 여기에 미리 쓰지 않는다.

## 준 컨텍스트
- `CLAUDE.md` (작업 규칙)
- `docs/architecture/role-boundaries.md`, `docs/architecture/context-diagram.md` (1-3 산출물)
- `docs/architecture/sync-failure-signals.md` (1-2 산출물)
- 제약: Java 21, Spring Boot 3.5, Gradle 멀티모듈 한 저장소, LocalStack `4.9` 태그 고정, 실제 AWS 자격증명 금지, 기동 명령과 종료·확인 명령을 함께 낼 것, 승인 우회 실행 금지

## 요청 예시
1. (1-4) `role-boundaries.md` 의 경계대로 Gradle 멀티모듈 골격을 만들어 줘. 모듈은 `api`, `functions:planner`, `functions:sender`, `mock-vendor` 다. 아직 기능 코드는 넣지 말고 빌드만 되게 해 줘.
2. (1-4) LocalStack 4.9 를 띄우고 내리는 스크립트를 만들어 줘. 이 셸의 aws 명령이 실제 AWS 로 나가지 않게 하는 환경 파일도 같이 만들고, 종료 뒤 남은 컨테이너가 없는지 확인하는 명령을 적어 줘.
