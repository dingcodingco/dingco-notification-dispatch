# teardown 체크리스트

판정은 "명령이 성공했다"가 아니라 "리소스 종류별 조회가 0개다"로 한다.

## 1. `scripts/local/teardown.sh` 가 하는 일 (순서대로)
| 순서 | 동작 | 이유 |
|---|---|---|
| 1 | `aws s3 rm s3://lab-payloads --recursive`, `s3://lab-recipients` 도 같게 | 객체가 남은 버킷은 스택 삭제 때 지워지지 않는다 |
| 2 | `aws cloudformation delete-stack --stack-name notify-lab` + `wait stack-delete-complete` | 큐 3개·테이블 3개·함수 2개·버킷 2개·이벤트 소스 매핑 |
| 3 | `aws s3 rb s3://lab-artifacts --force` | `deploy.sh` 가 패키징용으로 만든 버킷(스택 밖) |
| 4 | 남은 큐·테이블·함수·버킷·스택 수 출력 | 모두 `0개` 여야 한다 |
| 5 | `scripts/local/down.sh` → `docker stop kan-lab` | `--rm` 컨테이너라 안의 리소스와 Lambda 런타임 컨테이너가 함께 사라진다 |

## 2. teardown.sh 밖에서 할 일
```bash
scripts/local/api.sh stop          # Spring Boot (포트 8080)
scripts/local/vendor.sh stop       # mock-vendor (포트 8081)
```

## 3. 확인 명령
컨테이너를 내리기 전 출력(teardown.sh 4단계)에서:
```
남은 큐: 0개 / 남은 테이블: 0개 / 남은 함수: 0개 / 남은 버킷: 0개 / 남은 스택: 0개
```
내린 뒤:
```bash
docker ps --filter name=kan-lab --format '{{.Names}}'     # 빈 출력
docker ps --format '{{.Names}} {{.Image}}'                 # LocalStack 이 띄운 Lambda 런타임 컨테이너가 남지 않았는지
curl -s -o /dev/null localhost:8080 && echo api 남음 || echo api 없음
curl -s -o /dev/null localhost:8081/stats && echo vendor 남음 || echo vendor 없음
```

## 4. 기록칸
| 날짜 | 큐 | 테이블 | 함수 | 버킷 | 스택 | 컨테이너 | api/vendor | 판정 |
|---|---|---|---|---|---|---|---|---|
| (녹화 후 채움) | | | | | | | | |

## 알려진 빈틈
- teardown.sh 는 CloudWatch 로그 그룹 수를 세지 않는다. 로컬에서는 컨테이너 종료로 함께 사라진다. 로그 그룹까지 조회로 남기려면 `down.sh` 전에 `aws logs describe-log-groups --query 'length(logGroups)'` 를 따로 실행한다.
- 실제 AWS 에서는 Lambda 가 만든 로그 그룹이 스택 삭제 뒤에도 남는다. 강사 측 실제 AWS 정리 절차는 별도로 적는다(`scripts/aws/README.md`).
- `scripts/local/reset.sh` 는 재실습 초기화(큐 비우기·테이블 재생성)이고 teardown 이 아니다.

## 변경 받는 기준
새 리소스를 추가하는 변경은 teardown.sh 로 지워지고 4단계 조회에 잡혀야 받는다. teardown 이 빠진 제안은 reject 한다(`CLAUDE.md`).
