#!/usr/bin/env bash
# fixtures/payloads 를 S3 lab-payloads 버킷에 올린다. repair/ 는 올리지 않는다(6-5 에서 따로 올림).
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
aws s3 sync fixtures/payloads s3://lab-payloads/ --only-show-errors
echo "payload $(aws s3 ls s3://lab-payloads/ --recursive | wc -l | tr -d ' ')개 업로드"
