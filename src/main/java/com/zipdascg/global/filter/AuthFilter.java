package com.zipdascg.global.filter;

import com.zipdascg.global.jwt.JwtConfig;
import com.zipdascg.global.jwt.JwtProvider;
import com.zipdascg.global.response.GlobalResponseDTO;
import com.zipdascg.global.response.constant.CustomResponseCode;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,@NonNull GatewayFilterChain chain) {
        try{
            Optional<String> optionalToken = jwtProvider.extractAccessToken(exchange);

            if(optionalToken.isEmpty()){
                return chain.filter(exchange);
            }

            Claims claims = jwtProvider.extractClaims(optionalToken.get());
            ServerHttpRequest serverRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> httpHeaders.remove(jwtConfig.headerKey()))
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();
            return chain.filter(exchange.mutate().request(serverRequest).build());
        }catch (Exception e){
            return this.unauthorized(exchange);
        }


    }
    private Mono<Void> unauthorized(ServerWebExchange exchange){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(CustomResponseCode.INVALID_TOKEN_ERROR.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = objectMapper.writeValueAsBytes(GlobalResponseDTO.from(CustomResponseCode.INVALID_TOKEN_ERROR));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
