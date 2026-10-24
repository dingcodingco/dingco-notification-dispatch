#!/usr/bin/env bash
# 사용: scripts/measure/tenant-wait-report.sh [--since <epoch초>]
# Sender 의 [sender-trace] 줄을 테넌트별로 묶어, 발행(SentTimestamp)부터 처리까지 걸린 대기시간을 본다.
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
START=(); [ "${1:-}" = --since ] && START=(--start-time "${2}000")
aws logs filter-log-events --log-group-name /aws/lambda/sender ${START[@]+"${START[@]}"} \
  --query 'events[].message' --output text | tr '\t' '\n' | grep '^\[sender-trace\]' | python3 -c '
import re, sys, collections
w = collections.defaultdict(list)
for line in sys.stdin:
    m = re.search(r"tenantId=(\S+) .*waitMs=(-?\d+) result=(SENT|SKIPPED_DUPLICATE|RECOVERED_FROM_VENDOR)", line)
    if m: w[m.group(1)].append(int(m.group(2)))
def pct(v, p): v = sorted(v); return v[min(len(v) - 1, int(len(v) * p))]
head = "tenantId"
print(f"{head:<12} 처리 건수   p50 대기(ms)   p95 대기(ms)   최대(ms)")
for t in sorted(w):
    v = w[t]; print(f"{t:<12} {len(v):>8}   {pct(v,.5):>12}   {pct(v,.95):>12}   {max(v):>8}")
'
