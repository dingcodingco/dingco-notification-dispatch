#!/usr/bin/env bash
# 사용: scripts/replay/dispatch-campaign.sh <tenantId> <수신자 수> [X-Trace-Id]
# 공개 API 로 캠페인 생성 → presigned URL 로 수신자 CSV 업로드 → 발송 접수까지 한 번에 한다.
# NO_DISPATCH=1 이면 업로드까지만 하고 campaignId 를 출력한다(접수는 k6 등으로 따로).
set -euo pipefail
cd "$(dirname "$0")/../.."
TENANT=${1:?tenantId}; N=${2:?수신자 수}; TRACE=${3:-}
HDR=(); [ -n "$TRACE" ] && HDR=(-H "X-Trace-Id: $TRACE")
mkdir -p build
created=$(curl -s ${HDR[@]+"${HDR[@]}"} -X POST localhost:8080/campaigns -H 'Content-Type: application/json' \
  -d "{\"tenantId\":\"$TENANT\",\"name\":\"lab-$N\",\"channel\":\"SMS\",\"template\":\"안녕하세요 {name}님\"}")
id=$(printf '%s' "$created" | python3 -c 'import json,sys; print(json.load(sys.stdin)["campaignId"])')
url=$(printf '%s' "$created" | python3 -c 'import json,sys; print(json.load(sys.stdin)["uploadUrl"])')
scripts/data/gen-recipients.sh "$N" "$TENANT" > build/$id.csv
curl -s -o /dev/null -w "업로드 %{http_code}\n" -X PUT -H 'Content-Type: text/csv' --data-binary @build/$id.csv "$url"
if [ -n "${NO_DISPATCH:-}" ]; then echo "campaignId=$id (업로드까지만)"; exit 0; fi
curl -s ${HDR[@]+"${HDR[@]}"} -o build/$id.dispatch.json -w "발송 접수 %{http_code} %{time_total}초\n" -X POST localhost:8080/campaigns/$id/dispatch
echo "campaignId=$id $(cat build/$id.dispatch.json)"
