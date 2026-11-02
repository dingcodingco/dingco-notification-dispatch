#!/usr/bin/env bash
# 사용: scripts/ops/trace-lookup.sh <traceId>
# 같은 traceId 를 API 로그 → Planner 로그 → Sender 로그 → 결과 원장 순서로 찾는다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
T=${1:?traceId}
echo "== API (api/build/api.log)"; grep -F "[$T]" api/build/api.log || true
for fn in planner sender; do
  echo "== $fn"
  aws logs filter-log-events --log-group-name /aws/lambda/$fn --filter-pattern "\"$T\"" \
    --query 'events[].message' --output text | tr '\t' '\n' | grep -F "$T" | head -5 || true
done
echo "== 결과 원장 send-results (traceId=$T)"
aws dynamodb scan --table-name send-results --filter-expression 'traceId = :t' \
  --expression-attribute-values "{\":t\":{\"S\":\"$T\"}}" --select COUNT --query Count --output text
