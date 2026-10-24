#!/usr/bin/env bash
# 사용: scripts/local/vendor.sh start [지연ms] [초당한도(0=무제한)] | stop | restart ...
# 모의 발송사를 이 Mac 에서 띄운다(포트 8081). Lambda 는 host.docker.internal:8081 로 부른다.
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p build
case "${1:-start}" in
  stop) pkill -f com.dingco.dispatch.mockvendor.MockVendorServer >/dev/null 2>&1 && echo "mock-vendor 종료" || echo "mock-vendor 없음" ;;
  start|restart)
    pkill -f com.dingco.dispatch.mockvendor.MockVendorServer >/dev/null 2>&1 || true
    [ -x mock-vendor/build/install/mock-vendor/bin/mock-vendor ] || ./gradlew -q :mock-vendor:installDist
    MOCK_VENDOR_LATENCY_MS=${2:-20} MOCK_VENDOR_RATE_LIMIT=${3:-0} nohup mock-vendor/build/install/mock-vendor/bin/mock-vendor > build/mock-vendor.log 2>&1 &
    until curl -s localhost:8081/stats >/dev/null; do sleep 0.3; done
    head -1 build/mock-vendor.log ;;
esac
