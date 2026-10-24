# 테넌트 공정성 측정 증거

환경: LocalStack 4.9 (로컬), 저장소 `main` 브랜치. 실AWS 미검증.
원본 출력: 교재 작업 폴더 `assets/raw/s7-*.txt`
조건: tenant-a 300건 발송 접수 직후 tenant-b 20건, 모의 발송사 지연 20ms·속도 제한 없음, 배치 5

## 로컬 측정 (7-1 → 7-4)
| 조건 | tenant-a p50 / p95 (ms) | tenant-b p50 / p95 (ms) | tenant-b 완료 |
|---|---|---|---|
| fair queue 끔 | 50,228 / 92,615 | 98,690 / 102,029 | 약 118초 |
| fair queue 켬 (`MessageGroupId=tenantId`) | 43,053 / 80,368 | 86,097 / 87,719 | 약 98초 |

## 해석
- 두 경우 모두 tenant-b 는 tenant-a 가 거의 끝난 뒤에 처리됐다. 조용한 테넌트가 큰 테넌트 뒤에 줄을 선다.
- LocalStack 4.9 에는 fair queue 스케줄링이 없다. 켬/끔 차이는 실행 편차로 보고 개선으로 판정하지 않는다.
- 로컬에서 확인한 것: 발행 메시지에 `MessageGroupId: tenant-b` 가 실린다(7-2).
- 공정성 효과(tenant-b 대기 단축)는 실제 AWS 에서 같은 조건으로 측정해 판정한다.
