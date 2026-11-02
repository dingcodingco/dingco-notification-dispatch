#!/usr/bin/env bash
# 사용: scripts/replay/send-batch.sh <send-message-batch 엔트리 json 파일>
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
awslab sqs send-message-batch --queue-url "$(queue_url notify-send-requested)" \
  --entries "file://$1" --query 'Successful[].[Id,MessageId]' --output text
