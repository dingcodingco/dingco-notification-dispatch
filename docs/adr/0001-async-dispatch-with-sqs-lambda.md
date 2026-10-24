# ADR-0001. 발송을 SQS·Lambda 비동기 경로로 분리한다

- 상태: 채택
- 관련: `docs/architecture/role-boundaries.md`, `docs/architecture/async-requirements.md`

## 맥락
동기 baseline(`BaselineController`, `POST /baseline/sync-dispatch`)은 요청 스레드 안에서 수신자 수만큼 발송사를 차례로 부른다. 응답 시간이 수신자 수에 묶이고, 클라이언트 timeout 뒤에도 서버 루프가 계속 돌아 부분 성공이 생기며, 호출자가 재시도하면 중복 발송된다(`sync-failure-signals.md`, 측정값은 `evidence/sync-timeout-baseline.md`).

## 결정
- Spring Boot 는 공개 REST 3개와 **접수**만 맡는다. `POST /campaigns/{id}/dispatch` 는 상태를 `QUEUED` 로 바꾸고 계획 메시지 1건을 `notify-dispatch-planned` 에 넣은 뒤 `202` 로 응답한다.
- Planner Lambda 가 수신자 CSV 를 읽어 수신자마다 `NotificationSendRequested` 를 `notify-send-requested` 에 넣는다.
- Sender Lambda 가 멱등 처리 뒤 발송사를 부르고 결과 원장 `send-results` 에 남긴다. 3번 받고도 실패한 메시지는 `notify-send-requested-dlq` 로 간다.
- 인프라는 `template.yaml`(SAM) 하나로 정의하고, 로컬은 LocalStack 4.9 에 배포한다.

## 대안
| 대안 | 채택하지 않은 이유 |
|---|---|
| Spring Boot 안에서 스레드 풀·`@Async` 로 발송 | 응답은 빨라지지만 재시도·격리·진행 상태가 프로세스 메모리에 있어 재시작하면 잃는다 |
| Lambda 가 공개 API 까지 맡기 | 입력 검증·상태 조회·계약이 워커 코드에 흩어진다 |
| Planner 없이 API 가 수신자별 메시지를 직접 발행 | 접수 응답 시간이 다시 수신자 수에 묶인다 |

## 결과
- 접수 응답이 수신자 수와 분리된다(V1, `perf/k6/dispatch-accept.js`).
- 전달은 at-least-once 가 된다. 같은 메시지가 여러 번 올 수 있어서 Sender 멱등 처리가 필요하다(ADR-0002).
- 발송 완료는 응답으로 알 수 없고, `GET /campaigns/{id}` 의 원장 집계로 본다.
- 이 결정이 보장하지 않는 것: 발송 완료 시각. 소비 속도는 발송사 한도와 동시 실행 상한에 따른다(`concurrency-budget.md`).
