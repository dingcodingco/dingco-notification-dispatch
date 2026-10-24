# request-context — 02-contracts (Section 3)

> 촬영 전 초안: 실제 요청 원문과 AI 출력은 촬영 세션에서 채운다.

## 준 컨텍스트
- `docs/architecture/async-requirements.md` (R1·R2)
- `docs/contracts/rest-contract-decisions.md` (3-1·3-2 산출물: 공개 REST 3개, 업로드 정보 결정)
- 제약: 공개 경로는 3개를 넘기지 않는다, 별도 upload API 를 만들지 않는다, dispatch 는 수신자 수와 관계없이 접수만 한다, 테스트 없이 완료라고 하지 않는다

## 요청 예시
1. (3-3) `rest-contract-decisions.md` 의 표대로 OpenAPI 3.0 명세를 써 줘. 응답 필드 이름은 표와 한 글자도 다르지 않게 하고, 409 가 나는 두 경우를 description 에 적어 줘.
2. (3-4) 명세대로 캠페인 생성과 상태 조회를 구현해 줘. 생성 응답에 presigned PUT URL·objectKey·만료 시각을 담고, LocalStack 리소스로 도는 테스트를 같이 만들어 줘.
3. (3-5) 발송 접수를 구현해 줘. 수신자 파일 존재 확인 → 상태 DRAFT→QUEUED → 계획 큐에 메시지 1건 순서로 하고, 이미 시작한 캠페인을 동시에 두 번 접수해도 메시지가 1건만 나가게 해 줘.
