#!/usr/bin/env bash
# 사용: scripts/measure/redelivery-summary.sh [검색어]
# Sender 로그를 키별로 묶어 "몇 번 받았고 결과가 무엇이었나"를 본다.
# 부분 배치 실패 보고의 판정 기준: DLQ 건수가 아니라 "정상 메시지까지 다시 받았는가".
# (배치 묶음은 실행마다 달라서 DLQ 건수는 실행마다 달라질 수 있다)
set -euo pipefail
cd "$(dirname "$0")/../.."
scripts/measure/sender-logs.sh "${1:-.}" | python3 -c '
import re, sys, collections
recv = collections.defaultdict(int); results = collections.defaultdict(list)
for line in sys.stdin:
    m = re.search(r"idempotencyKey=(\S+) receiveCount=(\d+) result=(\w+)", line)
    if not m: continue
    k, n, r = m.group(1), int(m.group(2)), m.group(3)
    recv[k] = max(recv[k], n); results[k].append(r)
head = "idempotencyKey"
print(f"{head:<36} 최대수신  결과")
normal = [k for k in recv if "FAILED" not in results[k]]
failed = [k for k in recv if "FAILED" in results[k]]
for k in sorted(recv):
    joined = ",".join(dict.fromkeys(results[k]))
    print(f"{k:<36} {recv[k]:<8}  {joined}")
again = [k for k in normal if recv[k] > 1]
print(f"정상 메시지 {len(normal)}건 중 다시 받은 것: {len(again)}건")
print(f"실패 메시지 {len(failed)}건의 최대 수신: {max([recv[k] for k in failed], default=0)}회")
print("판정: " + ("정상 메시지까지 재전달됨(배치 전체 재시도)" if again else "실패한 메시지만 재전달됨"))
'
