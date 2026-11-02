#!/usr/bin/env bash
# 결과 원장(send-results) = 모의 발송사가 받은 발송 요청 기록. idempotencyKey 별 발송 수를 센다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
echo "발송 수  idempotencyKey"
awslab dynamodb scan --table-name send-results --query 'Items[].idempotencyKey.S' --output text \
  | tr '\t' '\n' | grep -v -e '^$' -e '^None$' | sort | uniq -c || true
echo "원장 전체 행 수: $(awslab dynamodb scan --table-name send-results --select COUNT --query Count --output text)"
