# request-context — checkpoint/04-resilience (3-5~3-9, 4-1~4-4)

> 파일럿 상태: 아래 요청은 촬영 때 칸이 Claude Code 에 실제로 입력하고, 그 세션의 출력으로 diff-summary·decision 을 다시 채운다.
> 현재 저장소의 `CheckThenPutIdempotencyStore` 는 reject 사례(칸 쇼츠 대본 03)를 재현하려고 교재 제작 측이 넣은 구현이다.

## 준 컨텍스트
- `contracts/messages/notification-send-requested.json` (메시지 v1)
- 3-5 재현 결과: 같은 idempotencyKey 2건 → 원장 2
- 제약: 발송사 호출 전 중복을 걸러낼 것, SQS MessageId 를 키로 쓰지 말 것, 테스트 없이 완료라고 하지 말 것

## 요청
1. (3-6) Sender 에 멱등 저장소를 붙여 줘. DynamoDB `idempotency-keys` 를 쓰고, 보관 기간과 발송 실패 시 처리도 제안해 줘.
2. (3-7) 같은 키 두 건이 차례로 올 때와 동시에 올 때를 각각 검증하는 테스트를 만들어 줘. 동시 케이스는 두 스레드 시작 시점을 맞춰 줘.
3. (3-8) `docs/reliability/retry-dlq-policy.md`, `SenderHandler.java`, `template.yaml` 세 파일을 주고:
   세 파일을 읽고 duplicate / 선점 중 / poison / 429 가 코드에서 각각 어느 분기로 가는지 표로 정리해줘.
   batchItemFailures 에 성공 메시지 ID가 들어가거나 예외를 삼켜 성공으로 넘기는 경로가 있는지 찾아줘.
   부분 배치 실패 보고 끔/켬을 "정상 메시지까지 다시 받았는가"로 판정하는 실패 테스트 목록을 제안해줘.
   코드는 고치지 말고 결과만 보고해줘.
4. (4-2) `evidence/tenant-fairness-run.md` 의 개선 전 표와 `template.yaml` 을 주고:
   tenant-b 20건이 tenant-a 300건 뒤에 줄 서서 기다렸어. 발송 순서는 필요 없고, 조용한 테넌트의 대기시간만 줄이면 돼.
   메시지 계약 v1 은 바꾸지 말고 방법을 제안해줘. 코드는 고치지 말고 제안만 보고해줘.
