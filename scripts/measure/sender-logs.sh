#!/usr/bin/env bash
# 사용: scripts/measure/sender-logs.sh [--since <epoch초>] [검색어]
# Sender Lambda 가 남긴 배치·메시지 처리 로그만 시간순으로 본다.
#   --since : 그 시각 이후 로그만 (예: T0=$(date +%s) 로 기록해 둔 값)
#   검색어  : 그 문자열이 든 줄만 (예: cmp-0003)
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
START=()
if [ "${1:-}" = --since ]; then START=(--start-time "${2}000"); shift 2; fi
aws logs filter-log-events --log-group-name /aws/lambda/sender ${START[@]+"${START[@]}"} \
  --query 'events[].message' --output text | tr '\t' '\n' \
  | grep -E '^\[sender\] (batch|messageId)' | grep -E -- "${1:-.}" || true
