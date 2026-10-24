# request-context — main (Section 9)

> 촬영 전 초안: 9-1·9-2 는 Claude Code 를 쓰지 않고 사람이 체크포인트 기록을 읽는다. 9-3 의 실제 요청 원문과 AI 출력은 촬영 세션에서 채운다.

## 준 컨텍스트 (9-3)
- `docs/ai/checkpoints/*/decision.md`
- `docs/ai/failure-matrix.md`
- 8-3 incident note
- 제약: 저장소에 없는 사실을 추가하지 않는다, 모든 주장에 저장소 안 근거 파일을 붙인다, 보장 표현을 쓰지 않는다, LocalStack 증거와 강사 측 실제 AWS 증거를 나눠 적는다, teardown 절차를 빠뜨리지 않는다

## 요청 예시
1. (9-3) 위 문서만 근거로 ADR 3개 초안을 써 줘. 주제는 비동기 발송 분리, 조건부 쓰기 멱등 키, fair queue 대 FIFO 다. 형식은 제목 `# ADR-000N. …`, 상태, 맥락, 결정, 대안, 결과이고, 결과 절에 이 결정이 보장하지 않는 것을 한 줄 넣어 줘.
2. (9-3) README 초안을 써 줘. 실행 순서, V1~V7 증거 경로, teardown 절차, 체크포인트 기록 목록을 넣어 줘.
