#!/usr/bin/env bash
# DLQ 에 쌓인 메시지를 꺼내 보지 않고(가시성 0초) 훑으며, 메시지마다 두 가지를 확인한다.
#   1) 결과 원장에 이미 발송 기록이 있는가  → 있으면 다시 넣어도 멱등 처리로 건너뛴다
#   2) payloadRef 가 가리키는 S3 객체가 있는가 → 없으면 먼저 복구해야 한다(그대로 넣으면 다시 DLQ 로 온다)
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
DLQ=$(queue_url notify-send-requested-dlq)
printf '%-36s %-6s %-8s %s\n' "idempotencyKey" "원장" "payload" "판단"
aws sqs receive-message --queue-url "$DLQ" --max-number-of-messages 10 --visibility-timeout 0 \
  --query 'Messages[].Body' --output json | python3 -c '
import json, sys
for b in json.load(sys.stdin) or []:
    m = json.loads(b)
    print(m["idempotencyKey"], m["payloadRef"])
' | sort -u | while read -r key ref; do
  sent=$(aws dynamodb scan --table-name send-results --select COUNT \
    --filter-expression 'idempotencyKey = :k' --expression-attribute-values "{\":k\":{\"S\":\"$key\"}}" \
    --query Count --output text </dev/null)
  if aws s3 ls "$ref" >/dev/null 2>&1 </dev/null; then has=있음; else has=없음; fi
  if [ "$sent" -gt 0 ]; then verdict="이미 발송됨 — 재투입해도 건너뜀"
  elif [ "$has" = 없음 ]; then verdict="payload 복구 후 재투입"
  else verdict="재투입 대상"; fi
  printf '%-36s %-6s %-8s %s\n' "$key" "$sent" "$has" "$verdict"
done
