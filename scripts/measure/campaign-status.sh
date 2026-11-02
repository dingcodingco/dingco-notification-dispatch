#!/usr/bin/env bash
# 사용: scripts/measure/campaign-status.sh <campaignId>
set -euo pipefail
curl -s "localhost:8080/campaigns/${1:?campaignId}" | python3 -m json.tool
