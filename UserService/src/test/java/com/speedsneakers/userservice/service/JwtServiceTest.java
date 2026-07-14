package com.speedsneakers.userservice.service;

import com.speedsneakers.userservice.model.entity.Role;
import com.speedsneakers.userservice.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-para-pruebas-unitarias-de-jwt-service";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);
    }

    private User buildUser() {
        User user = new User();
        user.setId(42L);
        user.setUsername("juan");
        user.setEmail("juan@example.com");
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void generateToken_incluyeElIdDelUsuarioComoSubject() {
        String token = jwtService.generateToken(buildUser());

        assertThat(jwtService.extractUserId(token)).isEqualTo("42");
    }

    @Test
    void generateToken_produceUnTokenValido() {
        String token = jwtService.generateToken(buildUser());

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_devuelveFalseParaTokenExpirado() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 20_000))
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_devuelveFalseParaTokenMalformado() {
        assertThat(jwtService.isTokenValid("esto-no-es-un-jwt")).isFalse();
    }

    @Test
    void isTokenValid_devuelveFalseParaTokenFirmadoConOtraClave() {
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "otra-clave-secreta-completamente-distinta-a-la-configurada".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("42")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otraClave)
                .compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void extractUserId_lanzaExcepcionParaTokenMalformado() {
        // extractUserId no captura errores de parseo, a diferencia de isTokenValid
        assertThatThrownBy(() -> jwtService.extractUserId("token-invalido"))
                .isInstanceOf(RuntimeException.class);
    }
}
