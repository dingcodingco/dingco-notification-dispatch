#!/usr/bin/env bash
# 사용: scripts/measure/wait-campaign.sh <campaignId> [최대 대기초(기본 600)]
# 캠페인 발송 수(sentCount)가 수신자 수(totalRecipients)에 닿을 때까지 기다린다. 10초마다 진행을 찍는다.
set -euo pipefail
ID=${1:?campaignId}; MAX=${2:-600}; start=$(date +%s); last=-1
while :; do
  read -r total sent < <(curl -s localhost:8080/campaigns/$ID | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d["totalRecipients"], d["sentCount"])')
  el=$(( $(date +%s) - start ))
  [ "$total" -gt 0 ] && [ "$sent" -ge "$total" ] && { echo "완료: $sent/$total — 약 ${el}초"; break; }
  [ $el -ge $MAX ] && { echo "${MAX}초 안에 끝나지 않음: $sent/$total"; exit 1; }
  [ $((el / 10)) -ne $last ] && { last=$((el / 10)); echo "진행: $sent/$total (${el}초)"; }
  sleep 2
done
