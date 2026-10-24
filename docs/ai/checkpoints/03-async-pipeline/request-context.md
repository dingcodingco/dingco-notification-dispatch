# request-context — 03-async-pipeline (Section 4·5)

> 촬영 전 초안: 실제 요청 원문과 AI 출력은 촬영 세션에서 채운다.

## 준 컨텍스트
- `docs/architecture/role-boundaries.md`, `docs/architecture/context-diagram.md`
- `contracts/messages/*.json`, `docs/contracts/message-contract-decisions.md`
- `docs/reliability/concurrency-budget.md` (Section 5)
- 제약: 모든 리소스는 `template.yaml` 안에 두고 `scripts/local/teardown.sh` 로 지운다, 본문은 메시지에 싣지 않는다, Planner 는 발송사를 부르지 않는다, Sender 는 실패를 삼키지 않는다

## 요청 예시
1. (4-1) 계획 큐, 발송 큐와 DLQ(3번 받고 실패하면 이동), 캠페인·멱등 키·결과 원장 테이블, 버킷 2개, Planner·Sender 함수를 SAM 템플릿으로 만들어 줘. 지우는 스크립트와 남은 리소스 수를 세는 명령도 같이 만들어 줘.
2. (4-2) 수신자 CSV 를 줄 단위로 읽어 수신자마다 `NotificationSendRequested` 를 10건씩 발행하는 Planner 를 만들어 줘. 파일 전체를 메모리에 올리지 말아 줘.
3. (4-4) 발송 메시지를 받아 모의 발송사를 부르고 결과 원장에 남기는 Sender 를 만들어 줘. 실패한 메시지는 실패로 드러내 줘.
4. (5-3) 발송사 HTTP 클라이언트에 인스턴스당 초당 요청 수 상한을 환경변수로 넣어 줘. 동시 실행 상한은 이벤트 소스 매핑 파라미터로 바꿀 수 있게 해 줘.
