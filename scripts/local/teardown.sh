#!/usr/bin/env bash
# 스택을 지우고, 남은 리소스가 없는지 목록으로 확인한 뒤 컨테이너를 내린다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
aws s3 rm s3://lab-payloads --recursive --only-show-errors || true
aws s3 rm s3://lab-recipients --recursive --only-show-errors || true
aws cloudformation delete-stack --stack-name notify-lab
aws cloudformation wait stack-delete-complete --stack-name notify-lab
aws s3 rb s3://lab-artifacts --force >/dev/null 2>&1 || true
echo "남은 큐: $(aws sqs list-queues --query 'length(QueueUrls || `[]`)' --output text)개"
echo "남은 테이블: $(aws dynamodb list-tables --query 'length(TableNames)' --output text)개"
echo "남은 함수: $(aws lambda list-functions --query 'length(Functions)' --output text)개"
echo "남은 버킷: $(aws s3api list-buckets --query 'length(Buckets)' --output text)개"
echo "남은 스택: $(aws cloudformation list-stacks --stack-status-filter CREATE_COMPLETE UPDATE_COMPLETE --query 'length(StackSummaries)' --output text)개"
scripts/local/down.sh
