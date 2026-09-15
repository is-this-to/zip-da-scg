package com.zipdascg.global.filter;

import com.zipdascg.global.context.TraceIdContext;
import com.zipdascg.global.error.custom.InvalidTokenException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                    .header("X-User-Type", claims.get("type", String.class))
                    .header("X-User-Role", extractRoles(claims))
                    .build();
            return chain.filter(exchange.mutate().request(serverRequest).build());
        }catch (Exception e){
            return this.unauthorized(exchange);
        }


    }

    // Gateway는 Role의 구조만 검사
    // MEMBER → X-User-Role: USER
    // ADMIN  → X-User-Role: CS_ADMIN,SALES_ADMIN
    private String extractRoles(Claims claims) {
        String type = claims.get("type", String.class);

        if ("MEMBER".equals(type)) {
            return extractMemberRole(claims);
        }

        if ("ADMIN".equals(type)) {
            return extractAdminRoles(claims);
        }

        throw new InvalidTokenException("지원하지 않는 사용자 type입니다.");
    }

    private String extractMemberRole(Claims claims) {
        Object roleClaim = claims.get("role");
        // role이 문자열 하나인가? 비어 있지 않은가?
        if (!(roleClaim instanceof String role)
            || role.isBlank()) {

            throw new InvalidTokenException("MEMBER role이 올바르지 않습니다.");
        }

        return role;
    }

    private String extractAdminRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        // roles가 배열인가? 하나 이상의 역할이 있는가?
        if (!(rolesClaim instanceof List<?> roles)
            || roles.isEmpty()) {

            throw new InvalidTokenException("ADMIN roles가 올바르지 않습니다.");
        }

        List<String> validatedRoles = new ArrayList<>();
        // 모든 항목이 문자열인가? 빈 문자열이 없는가?
        for (Object value : roles) {
            if (!(value instanceof String role)
                || role.isBlank()) {

                throw new InvalidTokenException("ADMIN roles가 올바르지 않습니다.");
            }

            validatedRoles.add(role);
        }
        // Gateway는 배열을 쉼표로 합쳐 하위 서비스에 전달
        return validatedRoles.stream()
            .distinct()
            .collect(Collectors.joining(","));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange){
        String traceId = TraceIdContext.get(exchange);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(CustomResponseCode.SCG_INVALID_TOKEN_ERROR.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(
                TraceIdContext.HEADER_NAME,
                traceId
        );

        byte[] bytes = objectMapper.writeValueAsBytes(
                GlobalResponseDTO.from(
                        CustomResponseCode.SCG_INVALID_TOKEN_ERROR,
                        traceId
                )
        );
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
