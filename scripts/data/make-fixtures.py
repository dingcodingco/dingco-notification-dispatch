#!/usr/bin/env python3
"""합성 fixture 생성기. 실제 수신자·연락처 없이 ID 만 쓴다.

- fixtures/payloads/<campaign>/<recipient>.json : S3 에 올릴 발송 본문
- fixtures/messages/*.json                      : SQS 에 넣을 NotificationSendRequested 메시지
- fixtures/repair/<campaign>/<recipient>.json   : 6-5 runbook 에서 누락 payload 를 복구할 때 올리는 파일

배치마다 수신자 1명은 payload 를 일부러 만들지 않는다(= poison). 복구 파일만 repair/ 에 둔다.
"""
import json, pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
FX = ROOT / "fixtures"


def msg(campaign, recipient, tenant="tenant-a", channel="SMS"):
    return {
        "campaignId": campaign,
        "tenantId": tenant,
        "recipientId": recipient,
        "channel": channel,
        "payloadRef": f"s3://lab-payloads/{campaign}/{recipient}.json",
        "idempotencyKey": f"{tenant}:{campaign}:{recipient}:{channel}",
        "traceId": f"trace-{recipient}",
    }


def payload(campaign, recipient):
    return {"template": "lab-notice-v1", "campaignId": campaign, "recipientId": recipient, "vars": {"name": recipient}}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=None) + "\n")


# 6-1 ~ 6-3: 단건
write(FX / "messages/send-requested-rcp-0001.json", msg("cmp-0001", "rcp-0001"))
write(FX / "payloads/cmp-0001/rcp-0001.json", payload("cmp-0001", "rcp-0001"))

# 6-4: 5건 배치 두 개. A 는 ReportBatchItemFailures 끔, B 는 켬.
for name, campaign, base, missing in [("a", "cmp-0002", 100, 103), ("b", "cmp-0003", 200, 203)]:
    entries = []
    for i in range(1, 6):
        rid = f"rcp-{base + i:04d}"
        entries.append({"Id": f"m{i}", "MessageBody": json.dumps(msg(campaign, rid), ensure_ascii=False)})
        target = FX / ("repair" if base + i == missing else "payloads") / campaign / f"{rid}.json"
        write(target, payload(campaign, rid))
    write(FX / f"messages/batch-{name}-{campaign}.json", entries)

print("fixtures 생성 완료")
