# ADR-0002. 조건부 쓰기와 선점 만료로 발송을 1번으로 수렴시킨다

- 상태: 채택
- 관련: `docs/reliability/idempotency-policy.md`, `evidence/duplicate-replay.md`

## 맥락
SQS Standard 큐와 이벤트 소스 매핑은 at-least-once 로 전달한다. `IdempotencyMode=off` 에서 같은 본문 2건을 발행하면 원장이 2가 된다(6-1). 재시도, DLQ 재처리, Planner 재실행도 같은 업무 요청을 다시 들여온다.

## 결정
- 키: `idempotencyKey = tenantId:campaignId:recipientId:channel`. SQS `MessageId` 는 쓰지 않는다.
- 저장소: DynamoDB `idempotency-keys`. 선점은 조건부 PutItem 한 번이다. 조건 `attribute_not_exists(idempotencyKey) OR (#s = :inprogress AND leaseUntil < :now)`.
- 상태 `IN_PROGRESS` → `COMPLETED`. 완료·해제는 owner 가 일치할 때만 한다.
- 선점 만료 `leaseUntil` = 30초(함수 제한 시간 15초보다 길게). 키 보관 TTL 7일.
- 순서: 검증 → payload 확인 → 선점 → 발송사 결과 조회 → 발송 → `COMPLETED`. 발송이 실패하면 자기 선점만 지운다.

## 대안
| 대안 | 채택하지 않은 이유 |
|---|---|
| 조회 후 저장(`CheckThenPutIdempotencyStore`) | 조회와 저장 사이의 틈으로 동시 2건이 모두 발송된다. `DuplicateSendTest` 동시 케이스 FAILED `expected: <1> but was: <2>` |
| 키에 MessageId 사용 | 재발행·재처리 중복을 못 막는다(6-2 reject) |
| 선점만 하고 만료 없음 | 선점 뒤 함수가 중단되면 키가 `IN_PROGRESS` 로 남아 영영 보내지 않는다 |
| 조건부 쓰기만, 결과 조회 없음 | 발송사는 처리했는데 응답이 타임아웃되면 다시 보낸다(`IdempotencyRecoveryTest`: 2번) |

## 결과
- 순차·동시 중복 모두 원장 1(`DuplicateSendTest`), 선점 뒤 중단은 만료 뒤 1, 응답 타임아웃은 결과 조회로 1(`IdempotencyRecoveryTest`).
- 메시지 수와 발송 결과 수는 다를 수 있다. 판정은 원장으로 한다.
- 이 결정이 보장하지 않는 것: 발송사가 멱등 키나 결과 조회를 지원하지 않으면 "응답 타임아웃"과 "발송 직후 완료 기록 전 중단"에서 2번 발송될 수 있다. 선점 만료 전까지는 재전달이 지연된다.
