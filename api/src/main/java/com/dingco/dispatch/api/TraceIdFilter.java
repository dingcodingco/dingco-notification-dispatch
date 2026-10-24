package com.dingco.dispatch.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 진입점에서 traceId 를 정한다. 요청에 X-Trace-Id 가 있으면 그대로, 없으면 새로 만든다. 응답 헤더와 로그에 싣는다. */
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String traceId = req.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("traceId", traceId);
        res.setHeader(HEADER, traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("traceId");
        }
    }

    public static String current() {
        return MDC.get("traceId");
    }
}
