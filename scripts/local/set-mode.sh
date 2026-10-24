#!/usr/bin/env bash
# 사용: scripts/local/set-mode.sh <off|check-then-put|conditional> <report|throw>
#   report = 실패한 메시지 ID만 돌려준다(ReportBatchItemFailures 켬)
#   throw  = 한 건이라도 실패하면 배치 전체를 실패시킨다(ReportBatchItemFailures 끔)
# 스택 파라미터만 바꿔 다시 배포한다. 콘솔이나 CLI 로 함수 설정을 직접 고치지 않는다.
# 다른 파라미터는 deploy.sh 와 같은 환경변수로 넘긴다(예: VENDOR_MODE=http).
set -euo pipefail
cd "$(dirname "$0")/../.."
REPORT=$([ "$2" = report ] && echo true || echo false)
scripts/local/deploy.sh "$1" "$REPORT"
