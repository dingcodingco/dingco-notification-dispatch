#!/usr/bin/env bash
# DLQ 의 메시지를 본 큐로 되돌린다(SQS 메시지 이동 작업). 목적지는 본 큐로 명시한다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
arn() { aws sqs get-queue-attributes --queue-url "$(queue_url "$1")" --attribute-names QueueArn --query Attributes.QueueArn --output text; }
SRC=$(arn notify-send-requested-dlq); DST=$(arn notify-send-requested)
aws sqs start-message-move-task --source-arn "$SRC" --destination-arn "$DST" >/dev/null
until [ "$(aws sqs list-message-move-tasks --source-arn "$SRC" --max-results 1 --query 'Results[0].Status' --output text)" != RUNNING ]; do sleep 1; done
aws sqs list-message-move-tasks --source-arn "$SRC" --max-results 1 \
  --query 'Results[0].[Status,ApproximateNumberOfMessagesMoved]' --output text | awk '{print "이동 작업: "$1", 옮긴 메시지 "$2"건"}'
