# 테넌트 공정성

## 문제
모든 테넌트가 발송 큐 `notify-send-requested` 하나를 공유한다. 한 테넌트가 대량 캠페인을 넣으면 그 메시지가 큐 앞쪽을 채우고, 뒤에 들어온 조용한 테넌트의 소량 캠페인이 그만큼 기다린다.

## 판정 기준
조용한 테넌트의 **대기시간**으로 판정한다. 메시지 수나 큐 깊이로 판정하지 않는다.
- 대기시간 = Sender 처리 시각 − SQS `SentTimestamp`. Sender 가 `[sender-trace] ... waitMs=` 로 남긴다(재전달이면 최초 발행부터).
- 집계: `scripts/measure/tenant-wait-report.sh [--since <epoch초>]` → 테넌트별 처리 건수, p50·p95·최대 대기(ms).
- 원장 확인: 각 테넌트의 원장 행 수가 캠페인 수신자 수와 같은지 본다(`GET /campaigns/{id}` 의 `sentCount`).

## 선택지
| 선택지 | 판단 |
|---|---|
| 테넌트별 큐 | 격리는 확실하지만 테넌트가 늘 때마다 큐·이벤트 소스 매핑·동시 실행 상한을 새로 나눠야 한다 |
| FIFO 큐 + MessageGroupId | **reject.** 아래 |
| **Standard 큐 + fair queue** (채택) | 큐 구조·DLQ·멱등 동작을 유지한 채 `MessageGroupId` 만 싣는다 |

### FIFO 전환 제안을 reject 한 이유
- 요구사항은 순서가 아니라 조용한 테넌트의 대기시간이다. 발송 순서 요구가 없다.
- FIFO 는 그룹 안 순서를 지키는 대신 처리량 구조와 처리 단위가 바뀐다. Section 5 의 처리량 예산과 Section 6 의 중복·DLQ 검증을 다시 해야 한다.
- 대체안: Standard 큐 + `MessageGroupId=tenantId`. 기록: `docs/adr/0003-fair-queue-over-fifo.md`.

## 구현
- `FAIR_QUEUE=true scripts/local/deploy.sh ...` → Planner 환경변수 `FAIR_QUEUE` → 배치 발행 엔트리에 `messageGroupId(tenantId)`.
- 확인: `scripts/measure/peek-message.sh` 로 `MessageGroupId` 가 본문 `tenantId` 와 같은지, 큐 이름에 `.fifo` 가 없는지 본다.

## 말하지 않는 것
fair queue 는 FIFO 순서를 보장하지 않으며 기아 방지를 보장하는 장치도 아니다. SQS 가 in-flight 가 몰린 그룹을 감지해 다른 그룹 메시지를 먼저 전달하려고 시도하는 동작이고, 효과는 측정값으로만 말한다.

## 측정 기록
| 조건 | 테넌트 | 처리 건수 | p50 | p95 | 최대 |
|---|---|---|---|---|---|
| 개선 전 (`FAIR_QUEUE=false`) | Section 7 측정으로 채움 | | | | |
| 개선 후 (`FAIR_QUEUE=true`) | 강사 측 실제 AWS 측정값으로 채움 | | | | |

LocalStack 4.9 는 `MessageGroupId` 를 저장하지만 테넌트별 공정 스케줄링이 없다(`scripts/aws/README.md`). 개선 효과는 실제 AWS 측정값으로만 채운다.
