# 강사 측 실AWS 확인

LocalStack 4.9 는 IaC 구성과 메시지 처리 흐름을 확인하는 데 쓴다. 아래 동작은 LocalStack 과 실AWS 가 다르거나 LocalStack 에 구현돼 있지 않아서, 강사가 실AWS 에서 따로 측정하고 결과를 교재에 "실AWS 측정"으로 싣는다. 학습자 실습은 아니다.

| 섹션 | 확인할 것 | LocalStack 4.9 | 스크립트 |
|---|---|---|---|
| 6-4·6-5 | 부분 배치 실패 보고 재전달, DLQ 재처리 뒤 MessageId·수신 횟수 | 재처리 뒤 MessageId 유지·수신 횟수 누적(DLQ 훑기 횟수만큼)으로 관찰됨. AWS 문서는 재처리 메시지를 새 MessageId 의 새 메시지로 취급 | `s6-redrive-check.sh` |
| 5 | 이벤트 소스 `MaximumConcurrency` 가 동시 실행 수를 실제로 제한하는지, 그때 발송 요청 속도 | 적용되지 않음(칸 확인) | Section 5 구현 때 작성 |
| 7 | `MessageGroupId`(=tenantId) fair queue 적용 전후 조용한 테넌트 대기시간 | 메시지에 값은 저장되나 테넌트별 공정 스케줄링 없음(칸 확인) | Section 7 구현 때 작성 |

실행 전 정할 것: 실습 전용 AWS 계정과 프로필, 비용 상한, 정리 책임자.
