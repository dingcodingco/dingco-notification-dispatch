package com.dingco.dispatch.sender;

import java.util.UUID;

/**
 * 검증 → payload 확인 → 선점 → (결과 조회) → 발송 → 완료 기록.
 *
 * payload 확인을 선점보다 먼저 한다. poison 메시지가 키를 선점한 채 실패하지 않게 하기 위해서다.
 * 결과 조회(recoverFromVendor): 선점에 성공해도 발송 전에 발송사에 이미 접수됐는지 먼저 묻는다.
 *   앞선 시도가 발송사에서는 처리됐는데 응답을 못 받았거나(타임아웃), 발송 직후 함수가 중단된 경우를 복구한다.
 * 발송이 실패하면 선점을 풀고 예외를 다시 던져 재시도가 다시 들어올 수 있게 한다.
 */
public class SenderService {
    public enum Outcome { SENT, SKIPPED_DUPLICATE, RECOVERED_FROM_VENDOR }

    private final IdempotencyStore store;
    private final VendorClient vendor;
    private final PayloadResolver payloads;
    private final boolean recoverFromVendor;

    public SenderService(IdempotencyStore store, VendorClient vendor, PayloadResolver payloads, boolean recoverFromVendor) {
        this.store = store;
        this.vendor = vendor;
        this.payloads = payloads;
        this.recoverFromVendor = recoverFromVendor;
    }

    /** 테스트용: payload 확인을 건너뛰고, 결과 조회는 하지 않는다. */
    public SenderService(IdempotencyStore store, VendorClient vendor) {
        this(store, vendor, ref -> { }, false);
    }

    public Outcome process(SendRequest request) {
        request.validate();
        payloads.requireExists(request.payloadRef());
        String owner = UUID.randomUUID().toString();
        switch (store.claim(request.idempotencyKey(), request.traceId(), owner)) {
            case ALREADY_COMPLETED:
                return Outcome.SKIPPED_DUPLICATE;
            case IN_PROGRESS_ELSEWHERE:
                throw new InProgressElsewhereException(request.idempotencyKey());
            case CLAIMED:
            default:
                break;
        }
        if (recoverFromVendor && vendor.alreadyAccepted(request.idempotencyKey())) {
            store.complete(request.idempotencyKey(), owner);
            return Outcome.RECOVERED_FROM_VENDOR;
        }
        try {
            vendor.send(request);
        } catch (RuntimeException e) {
            store.release(request.idempotencyKey(), owner);
            throw e;
        }
        store.complete(request.idempotencyKey(), owner);
        return Outcome.SENT;
    }
}
