package com.speedsneakers.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

/**
 * Valida el JWT de forma independiente al Gateway. El Gateway ya inyecta el
 * header X-User-Id, pero este servicio no debe confiar en él a ciegas: si
 * alguien lo alcanza sin pasar por el Gateway (ver puertos expuestos en
 * docker-compose.yml), un header X-User-Id falsificado permitiría suplantar
 * a cualquier usuario. Aquí se revalida la firma del token y se sobrescribe
 * el header con el userId real extraído del token.
 *
 * Se responde directamente con setStatus (no sendError): sendError dispara
 * el forward de error de Tomcat hacia /error, que vuelve a atravesar la
 * cadena de Spring Security como una petición anónima y termina
 * reemplazando el 401 por un 403 de AuthorizationFilter.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenCookie(request);

        if (token == null || token.isBlank()) {
            unauthorized(response, "Token no proporcionado");
            return;
        }

        String userId;
        try {
            userId = getClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response, "Token inválido o expirado");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList())
        );

        filterChain.doFilter(new UserIdOverrideRequestWrapper(request, userId), response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private String extractTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Claims getClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("Token expirado");
        }
        return claims;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fuerza X-User-Id al valor validado del token, para que un header
     * falsificado por el cliente nunca llegue al controlador.
     */
    private static class UserIdOverrideRequestWrapper extends HttpServletRequestWrapper {
        private final String userId;

        UserIdOverrideRequestWrapper(HttpServletRequest request, String userId) {
            super(request);
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return userId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(userId));
            }
            return super.getHeaders(name);
        }
    }
}
