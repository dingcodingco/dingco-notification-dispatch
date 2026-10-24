#!/usr/bin/env bash
# 사용: scripts/local/api.sh start | stop
# Spring Boot API 를 이 Mac 에서 띄운다(포트 8080). 로그는 api/build/api.log(traceId 포함).
set -euo pipefail
cd "$(dirname "$0")/../.."
source scripts/local/env.sh
case "${1:-start}" in
  stop) pkill -f 'build/libs/api.jar' >/dev/null 2>&1 && echo "api 종료" || echo "api 없음" ;;
  start)
    pkill -f 'build/libs/api.jar' >/dev/null 2>&1 || true
    [ -f api/build/libs/api.jar ] || ./gradlew -q :api:bootJar
    mkdir -p build
    (cd api && exec nohup java -jar build/libs/api.jar > ../build/api-console.log 2>&1) </dev/null >/dev/null 2>&1 &
    # AWS 를 부르지 않는 경로로 포트만 확인한다(LocalStack 이 꺼져 있어도 기동 판정이 멈추지 않게).
    for i in $(seq 1 120); do curl -s -m 2 -o /dev/null localhost:8080/ && break; sleep 0.5; done
    curl -s -m 2 -o /dev/null localhost:8080/ || { echo "api 기동 실패: build/api-console.log 확인"; exit 1; }
    echo "api 기동 완료 (포트 8080)" ;;
esac
