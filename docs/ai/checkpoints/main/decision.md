# decision — main

판정 기준만 적는다. 실제 판정은 촬영 때 채운다.

| 강 | 대상 | accept | edit | reject | 판정 |
|---|---|---|---|---|---|
| 4-11 | ADR 초안 3개 | 모든 주장이 저장소 근거 파일로 이어짐 | 측정값 없는 주장, 보장 표현, LocalStack/실제 AWS 구분 누락이 섞임 | 결정 근거를 바꿈(예: 로컬 결과를 실제 AWS 개선으로 적음) | 촬영 때 채움 |
| 4-11 | README 초안 | 실행 순서·V1~V7 증거·teardown·기록 목록을 갖춤 | 링크 일부가 없는 파일을 가리킴 | teardown 절차가 없음 | 촬영 때 채움 |

> 최종 제출 기준: `docs/adr/` 3개 이상, `failure-matrix.md` 의 reject 3건 이상이 README 또는 ADR 에서 참조된다.
