package com.speedsneakers.orderservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-para-pruebas-unitarias-del-filtro-jwt-order";

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    @Test
    void doFilterInternal_devuelve401SiNoHayCookies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilterInternal_devuelve401SiElTokenEstaEnBlanco() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", ""));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilterInternal_devuelve401SiElTokenEsInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", "esto-no-es-un-jwt"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilterInternal_devuelve401SiElTokenEstaExpirado() throws Exception {
        String expiredToken = buildToken("42", -10_000);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", expiredToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilterInternal_continuaLaCadenaYSobrescribeXUserIdConTokenValido() throws Exception {
        String validToken = buildToken("42", 60_000);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", validToken));
        request.addHeader("X-User-Id", "999");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(((jakarta.servlet.http.HttpServletRequest) chain.getRequest()).getHeader("X-User-Id"))
                .isEqualTo("42");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("42");
    }
}
