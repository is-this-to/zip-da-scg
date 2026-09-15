package com.zipdascg.global.filter;

import com.zipdascg.global.context.TraceIdContext;
import lombok.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    @NonNull
    public Mono<Void> filter(
            @NonNull ServerWebExchange exchange,
            @NonNull GatewayFilterChain chain
    ) {
        String traceId = TraceIdContext.resolve(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(TraceIdContext.HEADER_NAME)
        );

        ServerHttpRequest tracedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(TraceIdContext.HEADER_NAME);
                    headers.set(
                            TraceIdContext.HEADER_NAME,
                            traceId
                    );
                })
                .build();

        ServerWebExchange tracedExchange = exchange
                .mutate()
                .request(tracedRequest)
                .build();

        tracedExchange.getAttributes().put(
                TraceIdContext.ATTRIBUTE_NAME,
                traceId
        );

        tracedExchange.getResponse()
                .getHeaders()
                .set(
                        TraceIdContext.HEADER_NAME,
                        traceId
                );

        return chain.filter(tracedExchange)
                .contextWrite(context -> context.put(
                        TraceIdContext.REACTOR_CONTEXT_KEY,
                        traceId
                ));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}