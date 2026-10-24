# request-context — checkpoint/04-resilience (Section 6)

> 파일럿 상태: 아래 요청은 촬영 때 칸이 Claude Code 에 실제로 입력하고, 그 세션의 출력으로 diff-summary·decision 을 다시 채운다.
> 현재 저장소의 `CheckThenPutIdempotencyStore` 는 reject 사례(칸 쇼츠 대본 03)를 재현하려고 교재 제작 측이 넣은 구현이다.

## 준 컨텍스트
- `contracts/messages/notification-send-requested.json` (메시지 v1)
- 6-1 재현 결과: 같은 idempotencyKey 2건 → 원장 2
- 제약: 발송사 호출 전 중복을 걸러낼 것, SQS MessageId 를 키로 쓰지 말 것, 테스트 없이 완료라고 하지 말 것

## 요청
1. (6-2) Sender 에 멱등 저장소를 붙여 줘. DynamoDB `idempotency-keys` 를 쓰고, 보관 기간과 발송 실패 시 처리도 제안해 줘.
2. (6-3) 같은 키 두 건이 차례로 올 때와 동시에 올 때를 각각 검증하는 테스트를 만들어 줘. 동시 케이스는 두 스레드 시작 시점을 맞춰 줘.
3. (6-4) 재시도·DLQ 정책 초안과 지금 테스트 목록을 보고, 빠진 실패 경로를 찾아 줘.
