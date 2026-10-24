package com.dingco.dispatch.sender;

/** payloadRef(s3://bucket/key) 가 가리키는 본문이 실제로 있는지 확인한다. */
public interface PayloadResolver {
    /** 본문이 없으면 PoisonMessageException. 몇 번을 다시 받아도 같은 이유로 실패한다. */
    void requireExists(String payloadRef);
}
