package com.dingco.dispatch.sender;

/** 다른 처리가 같은 키를 선점 중이다. 이 메시지는 나중에 다시 받는다. */
public class InProgressElsewhereException extends RuntimeException {
    public InProgressElsewhereException(String key) {
        super("다른 처리가 선점 중: " + key);
    }
}
