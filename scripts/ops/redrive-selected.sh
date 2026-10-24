#!/usr/bin/env bash
# 사용: scripts/ops/redrive-selected.sh <idempotencyKey> [idempotencyKey ...]
# DLQ 에서 지정한 키의 메시지만 골라 본 큐로 다시 보낸다(선별 재처리).
# SQS 의 메시지 이동 작업(StartMessageMoveTask)은 DLQ 전체를 옮기고 골라 옮기는 기능이 없어서,
# 받기 → 본 큐로 새로 보내기 → DLQ 에서 지우기를 직접 한다. 고르지 않은 메시지는 곧바로 다시 보이게 돌려 둔다.
# 다시 보낸 메시지는 새 메시지라 MessageId 가 새로 붙는다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
DLQ=$(queue_url notify-send-requested-dlq); MAIN=$(queue_url notify-send-requested)
WANT=" $* "; moved=0
for pass in 1 2 3 4 5; do
  msgs=$(aws sqs receive-message --queue-url "$DLQ" --max-number-of-messages 10 --visibility-timeout 30 --wait-time-seconds 1 \
    --query 'Messages[].[MessageId,ReceiptHandle,Body]' --output json)
  [ "$msgs" = "null" ] || [ "$msgs" = "[]" ] && break
  while IFS=$'\t' read -r mid rh body; do
    key=$(printf '%s' "$body" | python3 -c 'import json,sys; print(json.load(sys.stdin)["idempotencyKey"])')
    if [[ "$WANT" == *" $key "* ]]; then
      new=$(aws sqs send-message --queue-url "$MAIN" --message-body "$body" --query MessageId --output text </dev/null)
      aws sqs delete-message --queue-url "$DLQ" --receipt-handle "$rh" </dev/null
      echo "재투입 $key  DLQ $mid → 본 큐 $new"
      moved=$((moved+1)); WANT=${WANT/ $key / }
    else
      aws sqs change-message-visibility --queue-url "$DLQ" --receipt-handle "$rh" --visibility-timeout 0 </dev/null
    fi
  done < <(printf '%s' "$msgs" | python3 -c 'import json,sys; [print("\t".join(m)) for m in json.load(sys.stdin)]')
  [ -z "${WANT// /}" ] && break
done
echo "선별 재투입 ${moved}건"
[ -n "${WANT// /}" ] && echo "DLQ 에서 찾지 못한 키:$WANT" || true
