package com.prashanth.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter {

    private final JWTUtil jwtUtil;
    private final RouteValidator routeValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Determine if security is required
        boolean isApiSecured = routeValidator.isSecured.test(request);
        boolean isProductPost = request.getMethod() == HttpMethod.POST
                && request.getURI().getPath().contains("/api/v1/products");

        // 2. If it's a secured route OR a Product POST, validate the JWT
        if (isApiSecured || isProductPost) {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.isTokenValid(token)) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                // Extract and propagate claims
                String email = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                ServerHttpRequest modifiedRequest = exchange.getRequest()
                        .mutate()
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        }

        // 3. If it's an open endpoint (and not a Product POST), just pass through
        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}