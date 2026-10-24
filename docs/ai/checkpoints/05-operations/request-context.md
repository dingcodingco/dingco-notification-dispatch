# request-context — 05-operations (Section 8)

> 이 섹션에는 새 AI 개입이 없다. 앞선 개입이 남긴 실행 절차를 다시 열어 teardown 누락 여부를 판정한다. 판정 결과는 촬영 때 채운다.

## 다시 여는 기록
- `docs/ai/checkpoints/00-bootstrap/request-context.md`: 기동·종료 스크립트 요청(bootstrap)
- `docs/ai/checkpoints/03-async-pipeline/request-context.md`: SAM 템플릿·정리 스크립트 요청(async)

## 볼 것
- 실행·배포 절차 끝에 삭제 단계와 조회 단계가 있는가
- "필요하면 정리한다"처럼 판정 기준이 없는 문장이 있는가
- 스택 밖에 만든 리소스(`lab-artifacts` 버킷 등)까지 지우는가

## 함께 쓰는 문서
- `docs/operations/trace-observability.md` (8-1·8-2)
- `docs/operations/dlq-reprocess-runbook.md` (8-3 incident note 의 재처리 순서)
- `docs/operations/teardown-checklist.md` (8-4)
