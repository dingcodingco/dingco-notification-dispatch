#!/usr/bin/env bash
# 사용: scripts/measure/peek-message.sh [큐 이름(기본 notify-send-requested)]
# 메시지 1건을 지우지 않고 꺼내 본다(가시성 0초). 본문과 MessageGroupId 를 보여 준다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
aws sqs receive-message --queue-url "$(queue_url "${1:-notify-send-requested}")" --visibility-timeout 0 \
  --attribute-names MessageGroupId --query 'Messages[0].{MessageGroupId:Attributes.MessageGroupId,Body:Body}' --output json \
  | python3 -c 'import json,sys; d=json.load(sys.stdin); print("MessageGroupId:", d["MessageGroupId"]); print(json.dumps(json.loads(d["Body"]), ensure_ascii=False, indent=2))'
