package com.speedsneakers.gatewayservice.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-para-pruebas-unitarias-del-gateway-filter";

    private AuthenticationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    private String buildToken(String subject, long expiresInMillis) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMillis))
                .signWith(key)
                .compact();
    }

    private MockServerWebExchange buildExchange(String path, String tokenCookieValue) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (tokenCookieValue != null) {
            builder = builder.cookie(new HttpCookie("token", tokenCookieValue));
        }
        return MockServerWebExchange.from(builder.build());
    }

    @Test
    void filter_permiteRutasPublicasSinValidarToken() {
        MockServerWebExchange exchange = buildExchange("/userservice/auth/login", null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_devuelve401SiNoHayCookieToken() {
        MockServerWebExchange exchange = buildExchange("/orderservice/orders", null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_devuelve401SiElTokenEstaEnBlanco() {
        MockServerWebExchange exchange = buildExchange("/orderservice/orders", "");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_devuelve401SiElTokenEsInvalido() {
        MockServerWebExchange exchange = buildExchange("/orderservice/orders", "esto-no-es-un-jwt");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_devuelve401SiElTokenEstaExpirado() {
        String expiredToken = buildToken("42", -10_000);
        MockServerWebExchange exchange = buildExchange("/orderservice/orders", expiredToken);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_agregaXUserIdYContinuaLaCadenaConTokenValido() {
        String validToken = buildToken("42", 60_000);
        MockServerWebExchange exchange = buildExchange("/orderservice/orders", validToken);

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerWebExchange modifiedExchange = captor.getValue();
        assertThat(modifiedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
}
