package com.zipdascg.global.context;

import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;
import java.util.regex.Pattern;

public final class TraceIdContext {

    public static final String HEADER_NAME = "X-Trace-Id";

    public static final String ATTRIBUTE_NAME =
            TraceIdContext.class.getName() + ".traceId";

    public static final String REACTOR_CONTEXT_KEY = "traceId";

    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    private TraceIdContext() {
    }

    public static String resolve(String traceIdHeader) {
        if (traceIdHeader == null || traceIdHeader.isBlank()) {
            return generate();
        }

        String trimmedTraceId = traceIdHeader.trim();

        if (!TRACE_ID_PATTERN.matcher(trimmedTraceId).matches()) {
            return generate();
        }

        return trimmedTraceId;
    }

    public static String get(ServerWebExchange exchange) {
        Object traceId = exchange.getAttribute(ATTRIBUTE_NAME);

        if (traceId instanceof String value && !value.isBlank()) {
            return value;
        }

        return resolve(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        );
    }

    private static String generate() {
        return UUID.randomUUID().toString();
    }
}
