#!/usr/bin/env bash
# 사용: scripts/replay/send-message.sh <메시지 json 파일> [횟수]
# 같은 본문을 N번 발행한다. 생산자 재시도나 중복 전달로 같은 업무 요청이 두 번 들어온 상황을 만든다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
FILE=$1; N=${2:-1}
for i in $(seq 1 "$N"); do
  awslab sqs send-message --queue-url "$(queue_url notify-send-requested)" \
    --message-body "file://$FILE" --query MessageId --output text
done
