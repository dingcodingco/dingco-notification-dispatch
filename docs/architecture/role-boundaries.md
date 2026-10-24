# 책임 경계: Spring Boot 와 Lambda

## 경계 표
| 구성요소 | 맡는 것 | 맡지 않는 것 | 해소하는 징후(`sync-failure-signals.md`) |
|---|---|---|---|
| Spring Boot `api/` | 공개 REST 3개, 입력 검증, `traceId` 결정, 업로드 URL 발급, 발송 접수(상태 `QUEUED` + 계획 메시지 1건), 상태 조회 | 수신자 파일 읽기, 수신자별 발송, 발송사 호출 | 응답 시간 선형 증가, 클라이언트 timeout |
| Planner Lambda | 수신자 CSV 를 읽어 수신자별 발송 메시지로 펼치기(fan-out), `totalRecipients` 기록 | 발송사 호출, 멱등 판정 | 요청 스레드 점유 |
| Sender Lambda | 메시지 검증, payload 확인, 멱등 키 선점·완료, 발송사 결과 조회, 발송, 실패 ID 보고 | 수신자 목록 해석, 캠페인 상태 전이 | 부분 성공, 재시도 시 중복 |
| SQS (계획 큐·발송 큐·DLQ) | 완충, 건 단위 재시도, 처리 불가 메시지 격리 | 순서 보장, 중복 제거 | 부분 성공(건 단위로 나눔) |
| S3 `lab-recipients`·`lab-payloads` | 수신자 파일, 발송 본문(`payloadRef` 대상) | 상태 저장 | 큰 본문이 메시지·요청에 실리는 문제 |
| DynamoDB `campaigns`·`idempotency-keys`·`send-results` | 캠페인 상태, 멱등 키, 결과 원장 | 메시지 전달 | 재시도 시 중복(키 판정), 진행 조회 |
| `mock-vendor/` | 지연·429·통계 재현 | 실제 발송 | 발송사 한도 측정 |

## 경계를 넘지 않는 이유
- Spring Boot 가 발송까지 맡으면 응답 시간이 수신자 수에 묶인다. 접수 응답은 수신자 수와 관계없이 나와야 한다(`async-requirements.md` R1).
- Lambda 가 공개 API 를 맡으면 입력 검증·상태 조회·계약이 워커 코드에 흩어진다. 공개 계약은 `openapi/notification-api.yaml` 한 곳에서 관리한다.
- Planner 가 발송사를 직접 부르면 멱등·재시도·429 처리가 두 곳에 생긴다. 발송사 호출은 Sender 한 곳에만 둔다.
- Sender 가 캠페인 상태를 바꾸지 않는다. 진행은 결과 원장 행 수로 집계한다(`CampaignService.status`).

## 확인 방법
- 코드: `PlannerHandler.java` 에 발송사 클라이언트가 없고, `CampaignService.java` 에 발송사 호출이 없다.
- 예외: `BaselineController.java` 는 Section 2 비교용으로 요청 스레드에서 발송사를 부른다. 공개 계약 밖이다.
