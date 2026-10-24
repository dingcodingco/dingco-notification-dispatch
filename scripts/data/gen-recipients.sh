#!/usr/bin/env bash
# 사용: scripts/data/gen-recipients.sh <수신자 수> [접두사]
# 합성 수신자 ID 만 담은 CSV 를 표준 출력으로 낸다(실제 연락처 없음).
set -euo pipefail
N=${1:?수신자 수}; PREFIX=${2:-rcp}
echo recipientId
seq -f "${PREFIX}-%06g" 1 "$N"
