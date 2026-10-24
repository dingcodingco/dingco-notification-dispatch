# source scripts/local/env.sh — 실습 공통 환경.
# 자격증명은 LocalStack 전용 더미 값이다. AWS_ENDPOINT_URL 을 LocalStack 으로 고정해
# 이 셸에서 실행하는 aws 명령이 실제 AWS 로 나가지 않게 한다.
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-northeast-2
export AWS_PAGER=""
export LAB_ENDPOINT=http://localhost:4566
export AWS_ENDPOINT_URL=$LAB_ENDPOINT
awslab() { aws --endpoint-url="$LAB_ENDPOINT" "$@"; }
queue_url() { awslab sqs get-queue-url --queue-name "$1" --query QueueUrl --output text; }
