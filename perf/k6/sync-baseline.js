// 동기 baseline: 수신자 수를 단계별로 늘려 /baseline/sync-dispatch 를 부른다.
// 실행: docker run --rm -i --add-host=host.docker.internal:host-gateway grafana/k6 run - < perf/k6/sync-baseline.js
// 단계(수신자 수)와 클라이언트 timeout 은 아래 상수로 정한다. 결과 해석은 사람이 한다.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.API || 'http://host.docker.internal:8080';
const STEPS = (__ENV.STEPS || '10,100,300,600').split(',').map(Number);
const TIMEOUT = __ENV.TIMEOUT || '10s';

export const options = { vus: 1, iterations: 1 };

export default function () {
  for (const recipients of STEPS) {
    const res = http.post(`${BASE}/baseline/sync-dispatch`,
      JSON.stringify({ campaignId: `baseline-${recipients}`, recipients }),
      { headers: { 'Content-Type': 'application/json' }, timeout: TIMEOUT, tags: { recipients: String(recipients) } });
    const ok = check(res, { 'status 200': (r) => r.status === 200 });
    console.log(`recipients=${recipients} status=${res.status} duration_ms=${Math.round(res.timings.duration)} ${ok ? 'OK' : 'TIMEOUT/ERROR ' + (res.error || '')}`);
  }
}
