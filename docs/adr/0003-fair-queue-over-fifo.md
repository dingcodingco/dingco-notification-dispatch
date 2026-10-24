# ADR-0003. 테넌트 공정성에 FIFO 대신 Standard 큐의 fair queue 를 쓴다

- 상태: 채택(효과 측정값은 강사 측 실제 AWS 측정으로 채움)
- 관련: `docs/reliability/tenant-fairness.md`, `evidence/tenant-fairness-run.md`

## 맥락
모든 테넌트가 발송 큐 `notify-send-requested` 하나를 공유한다. 한 테넌트의 대량 캠페인이 큐를 채우면 조용한 테넌트의 소량 캠페인이 기다린다. 판정 기준은 조용한 테넌트의 대기시간(`[sender-trace] waitMs`, `scripts/measure/tenant-wait-report.sh`)이다. failure tests 개입에서 Claude Code 가 FIFO 큐 전환을 제안했다.

## 결정
- 큐는 Standard 로 유지하고, Planner 가 발행 엔트리에 `MessageGroupId = tenantId` 를 싣는다(`FAIR_QUEUE=true`).
- FIFO 전환 제안은 reject 한다.

## 대안
| 대안 | 채택하지 않은 이유 |
|---|---|
| FIFO 큐 + `MessageGroupId` | 요구사항은 순서가 아니라 대기시간이다. 처리량 구조·처리 단위가 바뀌어 Section 5 예산과 Section 6 중복·DLQ 검증을 다시 해야 한다 |
| 테넌트별 큐 | 테넌트가 늘 때마다 큐·이벤트 소스 매핑·동시 실행 상한을 새로 나눠야 한다 |
| 그대로 두기 | 조용한 테넌트 대기시간이 대량 테넌트의 적체에 묶인다 |

## 결과
- 큐 이름·DLQ·멱등 동작·처리량 예산이 그대로다. 바뀌는 것은 Planner 발행 엔트리의 필드 하나다.
- LocalStack 4.9 는 `MessageGroupId` 를 저장하지만 공정 스케줄링이 없다. 로컬에서는 값이 실리는지만 확인하고(`scripts/measure/peek-message.sh`), 대기시간 개선은 실제 AWS 측정값으로만 적는다.
- 이 결정이 보장하지 않는 것: FIFO 순서, 기아 방지. 효과는 측정값으로만 말한다.
