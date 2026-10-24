// dispatch 접수 판정(V1): 수신자 파일을 올린 캠페인에 발송 시작 요청을 보내고 응답 시간을 잰다.
// 실행: docker run --rm -i --add-host=host.docker.internal:host-gateway -e CAMPAIGN=<campaignId> grafana/k6 run - < perf/k6/dispatch-accept.js
// 수신자 수와 상관없이 접수(202)가 timeout 없이 오는지 본다. 발송 완료 건수가 아니라 접수 판정이다.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.API || 'http://host.docker.internal:8080';
export const options = {
  vus: 1, iterations: 1,
  thresholds: { http_req_duration: ['max<1000'], checks: ['rate==1'] },
};

export default function () {
  const res = http.post(`${BASE}/campaigns/${__ENV.CAMPAIGN}/dispatch`, null, { timeout: '10s' });
  check(res, { 'status 202': (r) => r.status === 202 });
  console.log(`status=${res.status} duration_ms=${Math.round(res.timings.duration)} body=${res.body}`);
}
