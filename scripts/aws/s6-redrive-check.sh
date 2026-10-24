#!/usr/bin/env bash
# 강사 측 실AWS 확인 (Section 6-4·6-5). 학습자 실습이 아니다.
# 확인할 것: (1) 부분 배치 실패 보고 켬 상태의 재전달 (2) DLQ 재처리(StartMessageMoveTask) 뒤 MessageId·ApproximateReceiveCount
# 사용: AWS_PROFILE=<실습 전용 계정 프로필> CONFIRM_REAL_AWS=yes scripts/aws/s6-redrive-check.sh
# 비용: Lambda·SQS·DynamoDB·S3 소량 호출(프리티어 범위로 예상). 끝나면 스택과 SAM 배포 버킷 객체를 지운다.
set -euo pipefail
cd "$(dirname "$0")/../.."
[ "${CONFIRM_REAL_AWS:-}" = yes ] || { echo "실AWS 에 리소스를 만든다. CONFIRM_REAL_AWS=yes 로 다시 실행"; exit 1; }
[ -n "${AWS_PROFILE:-}" ] || { echo "AWS_PROFILE 을 명시"; exit 1; }
[ -z "${AWS_ENDPOINT_URL:-}" ] || { echo "AWS_ENDPOINT_URL 이 설정돼 있다(LocalStack 셸). 새 셸에서 실행"; exit 1; }
export AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-ap-northeast-2} AWS_PAGER=""
STACK=notify-lab-aws-check
echo "계정: $(aws sts get-caller-identity --query Account --output text)  리전: $AWS_DEFAULT_REGION"

./gradlew -q :functions:sender:buildZip
sam deploy -t template.yaml --stack-name $STACK --capabilities CAPABILITY_IAM --resolve-s3 \
  --parameter-overrides IdempotencyMode=conditional ReportBatchItemFailures=true --no-confirm-changeset >/dev/null
trap 'echo "정리"; aws s3 rm s3://lab-payloads --recursive --only-show-errors || true; sam delete --stack-name '$STACK' --no-prompts >/dev/null' EXIT

qurl() { aws sqs get-queue-url --queue-name "$1" --query QueueUrl --output text; }
MAIN=$(qurl notify-send-requested); DLQ=$(qurl notify-send-requested-dlq)
aws s3 sync fixtures/payloads s3://lab-payloads/ --only-show-errors
T0=$(date +%s)
aws sqs send-message-batch --queue-url "$MAIN" --entries file://fixtures/messages/batch-b-cmp-0003.json --query 'Successful[].MessageId' --output text
echo "재전달·DLQ 이동 대기(약 90초)"; sleep 90

echo "== 1) Sender 로그 (켬 상태: 실패 메시지만 다시 받는가)"
aws logs filter-log-events --log-group-name /aws/lambda/sender --start-time ${T0}000 --query 'events[].message' --output text | tr '\t' '\n' | grep '^\[sender\] messageId' || true

echo "== 2) DLQ 메시지(이동 전) MessageId·ApproximateReceiveCount"
aws sqs receive-message --queue-url "$DLQ" --max-number-of-messages 10 --visibility-timeout 0 --attribute-names ApproximateReceiveCount \
  --query 'Messages[].[MessageId,Attributes.ApproximateReceiveCount]' --output text

echo "== 3) 누락 payload 복구 후 DLQ 전체 재처리"
aws s3 cp fixtures/repair/cmp-0003/rcp-0203.json s3://lab-payloads/cmp-0003/rcp-0203.json --only-show-errors
T1=$(date +%s)
aws sqs start-message-move-task --source-arn "$(aws sqs get-queue-attributes --queue-url "$DLQ" --attribute-names QueueArn --query Attributes.QueueArn --output text)" >/dev/null
sleep 45
echo "== 4) 재처리 뒤 Sender 로그(새 MessageId 인가, receiveCount 가 몇인가)"
aws logs filter-log-events --log-group-name /aws/lambda/sender --start-time ${T1}000 --query 'events[].message' --output text | tr '\t' '\n' | grep '^\[sender\] messageId' || true
