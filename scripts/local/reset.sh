#!/usr/bin/env bash
# 재실습 초기화: 본 큐·DLQ 를 비우고, 멱등 키 테이블과 결과 원장을 빈 상태로 다시 만들고, Sender 로그 그룹을 지운다.
# 큐·함수·이벤트 소스 매핑 자체는 그대로 둔다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
for q in notify-send-requested notify-send-requested-dlq; do
  awslab sqs purge-queue --queue-url "$(queue_url $q)"
done
for t in idempotency-keys send-results; do
  awslab dynamodb delete-table --table-name "$t" >/dev/null
  awslab dynamodb wait table-not-exists --table-name "$t"
done
awslab dynamodb create-table --table-name idempotency-keys --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=idempotencyKey,AttributeType=S --key-schema AttributeName=idempotencyKey,KeyType=HASH >/dev/null
awslab dynamodb create-table --table-name send-results --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=sendId,AttributeType=S AttributeName=idempotencyKey,AttributeType=S AttributeName=campaignId,AttributeType=S \
  --key-schema AttributeName=sendId,KeyType=HASH \
  --global-secondary-indexes 'IndexName=by-idempotency-key,KeySchema=[{AttributeName=idempotencyKey,KeyType=HASH}],Projection={ProjectionType=KEYS_ONLY}' \
                             'IndexName=by-campaign,KeySchema=[{AttributeName=campaignId,KeyType=HASH}],Projection={ProjectionType=KEYS_ONLY}' >/dev/null
for t in idempotency-keys send-results; do awslab dynamodb wait table-exists --table-name "$t"; done
awslab logs delete-log-group --log-group-name /aws/lambda/sender 2>/dev/null || true
echo "초기화 완료: 큐 2개 비움 / idempotency-keys, send-results 빈 테이블 / Sender 로그 그룹 삭제"
