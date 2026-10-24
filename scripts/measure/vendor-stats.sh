#!/usr/bin/env bash
# 모의 발송사가 받은 수·429 수·최대 초당 요청 수·테넌트별 받은 수
set -euo pipefail
curl -s localhost:8081/stats | python3 -m json.tool
