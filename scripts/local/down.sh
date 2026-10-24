#!/usr/bin/env bash
# --rm 으로 띄웠으므로 stop 만 하면 컨테이너와 안의 리소스가 모두 사라진다.
# LocalStack 이 띄운 Lambda 런타임 컨테이너도 함께 정리된다.
set -euo pipefail
docker stop kan-lab >/dev/null && echo 'kan-lab 종료'
