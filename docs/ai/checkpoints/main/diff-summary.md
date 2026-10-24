# diff-summary — main

> 현재 저장소는 `main` 단일 브랜치다. 아래 표는 이 단계에서 다루는 파일이다.

| 파일 | 변경 | 사람이 읽은 곳 |
|---|---|---|
| `docs/ai/verification-checklist.md` | 수용 전 확인 목록 | 항목마다 확인 방법이 실행 가능한지 |
| `docs/ai/failure-matrix.md` | 실패 유형 표, reject 3건 | 재현 방법의 스크립트가 실제로 있는지 |
| `docs/adr/0001-async-dispatch-with-sqs-lambda.md` | 비동기 분리 | 근거 경로 |
| `docs/adr/0002-idempotency-with-conditional-write-and-lease.md` | 멱등 키 | 남는 위험이 적혔는지 |
| `docs/adr/0003-fair-queue-over-fifo.md` | fair queue | 보장 표현이 없는지, LocalStack 한계 |
| `README.md` | 실행 순서·증거·teardown | V1~V7 링크가 모두 열리는지 |
| `docs/ai/checkpoints/*/` | 기록 4종 | |
