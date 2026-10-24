#!/usr/bin/env bash
# 사용: scripts/local/deploy.sh [IdempotencyMode] [ReportBatchItemFailures]
#   IdempotencyMode: off | check-then-put | conditional (기본 conditional)
#   ReportBatchItemFailures: true | false (기본 true)
# 환경변수로 더 바꿀 수 있다(없으면 기본값):
#   VENDOR_MODE=ledger|http  VENDOR_MAX_RPS=0  SENDER_MAX_CONCURRENCY=0|2..  SENDER_ENABLED=true|false  SENDER_BATCH_SIZE=5  FAIR_QUEUE=false|true
# template.yaml(SAM) 을 LocalStack 의 CloudFormation 으로 배포한다. 같은 스택 이름으로 다시 실행하면 파라미터만 바뀐다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
MODE=${1:-conditional}; REPORT=${2:-true}
for z in functions/sender/build/distributions/sender.zip functions/planner/build/distributions/planner.zip; do
  [ -f "$z" ] || { echo "먼저 ./gradlew buildZip 을 실행하세요 ($z 없음)"; exit 1; }
done

aws s3 mb s3://lab-artifacts >/dev/null 2>&1 || true
mkdir -p build
aws cloudformation package --template-file template.yaml --s3-bucket lab-artifacts \
  --output-template-file build/packaged.yaml >/dev/null 2>&1
aws cloudformation deploy --template-file build/packaged.yaml --stack-name notify-lab \
  --capabilities CAPABILITY_IAM --no-fail-on-empty-changeset \
  --parameter-overrides IdempotencyMode=$MODE ReportBatchItemFailures=$REPORT \
    VendorMode=${VENDOR_MODE:-ledger} VendorMaxRps=${VENDOR_MAX_RPS:-0} SenderMaxConcurrency=${SENDER_MAX_CONCURRENCY:-0} \
    SenderEnabled=${SENDER_ENABLED:-true} SenderBatchSize=${SENDER_BATCH_SIZE:-5} FairQueue=${FAIR_QUEUE:-false} >/dev/null
aws lambda wait function-updated-v2 --function-name sender
echo "스택: $(aws cloudformation describe-stacks --stack-name notify-lab --query 'Stacks[0].StackStatus' --output text)"
echo "Sender 환경: $(aws lambda get-function-configuration --function-name sender \
  --query 'Environment.Variables.[IDEMPOTENCY_MODE,REPORT_BATCH_ITEM_FAILURES]' --output text | tr '\t' ' ')"
echo "이벤트 소스 매핑: $(aws lambda list-event-source-mappings --function-name sender \
  --query 'EventSourceMappings[0].[BatchSize,MaximumBatchingWindowInSeconds,join(`,`,FunctionResponseTypes||[`없음`])]' --output text | tr '\t' ' ')"
if [ -n "${VENDOR_MODE:-}${VENDOR_MAX_RPS:-}${SENDER_MAX_CONCURRENCY:-}${SENDER_ENABLED:-}${SENDER_BATCH_SIZE:-}${FAIR_QUEUE:-}" ]; then
  echo "추가 설정: 발송사 ${VENDOR_MODE:-ledger} / 인스턴스당 속도 ${VENDOR_MAX_RPS:-0}(0=없음) / 동시 실행 상한 ${SENDER_MAX_CONCURRENCY:-0}(0=없음) / Sender 트리거 ${SENDER_ENABLED:-true} / 배치 ${SENDER_BATCH_SIZE:-5} / fair queue ${FAIR_QUEUE:-false}"
fi
