#!/usr/bin/env python3
"""처리량 예산 계산기(Section 5-3). 측정값이 아니라 입력한 값으로 계산한다.

사용: scripts/measure/budget.py --vendor-rps <발송사 초당 한도> --per-instance <인스턴스당 초당 요청> [--recipients 100000000]

- 동시 실행 상한(최대) = 발송사 한도 ÷ 인스턴스당 속도   (이보다 많이 띄우면 429)
- 전체 발송 속도       = min(발송사 한도, 동시 실행 수 × 인스턴스당 속도)
- 전체 소요 시간       = 수신자 수 ÷ 전체 발송 속도
"""
import argparse, math

p = argparse.ArgumentParser()
p.add_argument("--vendor-rps", type=float, required=True)
p.add_argument("--per-instance", type=float, required=True)
p.add_argument("--recipients", type=int, default=100_000_000)
a = p.parse_args()
max_conc = math.floor(a.vendor_rps / a.per_instance)
rate = min(a.vendor_rps, max_conc * a.per_instance)
sec = a.recipients / rate
print(f"입력: 발송사 한도 {a.vendor_rps:g}/초, 인스턴스당 {a.per_instance:g}/초, 수신자 {a.recipients:,}명")
print(f"동시 실행 상한(최대): {max_conc}  (발송사 한도 ÷ 인스턴스당 속도)")
print(f"전체 발송 속도: {rate:g}/초")
print(f"전체 소요 시간: {sec:,.0f}초 ≈ {sec/3600:,.1f}시간 ≈ {sec/86400:,.1f}일")
