#!/usr/bin/env bash
# LocalStack 4.9 컨테이너를 띄운다. latest/stable 태그는 라이선스 토큰을 요구하며 즉시 종료(exit 55)되므로 4.9 로 고정한다.
# Lambda 를 실행하려면 LocalStack 이 도커로 런타임 컨테이너를 띄워야 해서 docker.sock 을 마운트한다.
set -euo pipefail
# 직전 컨테이너가 아직 지워지는 중이면 같은 이름으로 못 띄우므로 먼저 확실히 지운다.
docker rm -f kan-lab >/dev/null 2>&1 || true
while docker ps -a --format '{{.Names}}' | grep -qx kan-lab; do sleep 1; done
docker run --rm -d --name kan-lab \
  -p 4566:4566 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e SERVICES=sqs,lambda,dynamodb,logs,s3,cloudformation,iam,sts \
  localstack/localstack:4.9 >/dev/null
printf 'LocalStack 기동 대기'
until curl -s localhost:4566/_localstack/health | grep -q '"sqs": "\(available\|running\)"'; do printf '.'; sleep 1; done
echo ' 준비 완료'
