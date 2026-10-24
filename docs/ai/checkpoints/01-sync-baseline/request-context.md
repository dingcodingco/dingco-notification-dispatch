# request-context — 01-sync-baseline (Section 2)

> 촬영 전 초안: 실제 요청 원문과 AI 출력은 촬영 세션에서 채운다.

## 준 컨텍스트
- `docs/architecture/sync-failure-signals.md`
- `mock-vendor/` 의 `/send`·`/stats`·`/reset` 동작
- 제약: baseline 은 공개 계약 밖 경로로 둔다, 동기 루프를 개선하지 말고 그대로 재현한다, 결과 해석은 스크립트에 넣지 않는다

## 요청 예시
1. (2-1) 요청 스레드 안에서 수신자 수만큼 모의 발송사 `/send` 를 차례로 부르는 `POST /baseline/sync-dispatch` 를 만들어 줘. 공개 API 명세에는 넣지 말고, 걸린 시간을 로그에 남겨 줘.
2. (2-3) `/baseline/sync-dispatch` 를 수신자 수 단계별로 부르는 k6 스크립트를 만들어 줘. 단계와 클라이언트 timeout 은 환경변수로 바꿀 수 있게 하고, 단계마다 상태 코드와 걸린 시간만 찍어 줘.
