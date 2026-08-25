package com.zipdascg.global.jwt;

import com.zipdascg.global.error.custom.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final JwtConfig jwtConfig;

    public JwtProvider(JwtConfig jwtConfig){
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.secret()));
    }

    public Optional<String> extractAccessToken(ServerWebExchange exchange){
        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(jwtConfig.headerKey());
        String prefix = jwtConfig.scheme() + " ";

        if(authorizationHeader == null || !authorizationHeader.startsWith(prefix)){
            return Optional.empty();
        }

        String accessToken = authorizationHeader.substring(prefix.length()).trim();

        return accessToken.isEmpty()? Optional.empty() : Optional.of(accessToken);
    }
    
    public Claims extractClaims(String token){
        try{
                    // 1. JWT 검증기 준비
            return Jwts.parser()
                    .verifyWith(this.secretKey)
                    .build()
                    // 2. 실제 token을 전달하여 검증하고 파싱
                    .parseSignedClaims(token)
                    // 3. 검증된 Payload 꺼내기
                    .getPayload()
                    ;
        }catch(ExpiredJwtException e){
            throw new InvalidTokenException("토큰이 만료됐습니다.");
        }catch(UnsupportedJwtException e){
            throw new InvalidTokenException("지원하지 않는 형식의 토큰입니다.");
        }catch(MalformedJwtException e){
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        }catch(JwtException | IllegalArgumentException e){
            throw new InvalidTokenException("토큰 검증에 실패했습니다.");
        }
    }



}
