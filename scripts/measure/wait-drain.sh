#!/usr/bin/env bash
# 본 큐의 보이는 메시지·처리 중 메시지가 모두 0 이 될 때까지 기다린다(최대 180초).
# 재전달은 가시성 제한 시간(20초)이 지나야 일어나므로, 실패가 섞이면 1분 넘게 걸린다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
Q=$(queue_url notify-send-requested); start=$(date +%s)
sleep 5
while :; do
  read -r v nv < <(aws sqs get-queue-attributes --queue-url "$Q" \
    --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
    --query 'Attributes.[ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible]' --output text)
  [ "$v" = 0 ] && [ "$nv" = 0 ] && break
  [ $(( $(date +%s) - start )) -gt 180 ] && { echo "180초 안에 비지 않음 (보임 $v / 처리 중 $nv)"; exit 1; }
  sleep 3
done
sleep 3   # 로그가 CloudWatch Logs 로 넘어갈 시간
echo "본 큐 비움 — 약 $(( $(date +%s) - start ))초"
