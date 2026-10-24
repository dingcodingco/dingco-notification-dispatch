#!/usr/bin/env bash
# 본 큐와 DLQ 에 남은 메시지 수(근사값). 보이는 메시지 / 처리 중(보이지 않는) 메시지.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
for q in notify-send-requested notify-send-requested-dlq; do
  awslab sqs get-queue-attributes --queue-url "$(queue_url $q)" \
    --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
    --query "Attributes.[\`$q\`,ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible]" --output text
done
