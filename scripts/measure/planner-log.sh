#!/usr/bin/env bash
# 사용: scripts/measure/planner-log.sh [campaignId] [최대 대기초(기본 120)]
# Planner Lambda 의 요약 줄([planner] ...)이 로그에 찍힐 때까지 기다렸다가 마지막 줄을 보여 준다.
# Java Lambda 는 첫 실행(콜드 스타트)에 수십 초가 걸릴 수 있어서, 로그 그룹이 아직 없어도 기다린다.
set -uo pipefail
source "$(dirname "$0")/../local/env.sh"
ID=${1:-}; MAX=${2:-120}; start=$(date +%s)
while :; do
  L=$(aws logs filter-log-events --log-group-name /aws/lambda/planner --query 'events[].message' --output text 2>/dev/null \
      | tr '\t' '\n' | grep '^\[planner\]' | grep -- "${ID}" | tail -1)
  [ -n "$L" ] && { echo "$L"; exit 0; }
  [ $(( $(date +%s) - start )) -ge "$MAX" ] && { echo "${MAX}초 안에 planner 로그가 없음"; exit 1; }
  sleep 3
done
