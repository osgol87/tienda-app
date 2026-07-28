package com.speedsneakers.userservice.controller;

import com.speedsneakers.userservice.model.request.LoginRequest;
import com.speedsneakers.userservice.model.request.RegisterRequest;
import com.speedsneakers.userservice.model.response.AuthResponse;
import com.speedsneakers.userservice.service.AuthService;
import com.speedsneakers.userservice.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, jwtService);
    }

    @Test
    void register_devuelve201YCookieConToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("juan");
        request.setEmail("juan@example.com");
        request.setPassword("secreto1");

        when(authService.register(request)).thenReturn(new AuthResponse("juan", "juan@example.com", "token-123"));

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> response = authController.register(request, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getToken()).isNull();
        assertThat(response.getBody().getUsername()).isEqualTo("juan");

        Cookie cookie = servletResponse.getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("token-123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void login_devuelve200YCookieConToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@example.com");
        request.setPassword("secreto1");

        when(authService.login(request)).thenReturn(new AuthResponse("juan", "juan@example.com", "token-123"));

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ResponseEntity<AuthResponse> response = authController.login(request, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNull();

        Cookie cookie = servletResponse.getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("token-123");
    }

    @Test
    void logout_expiraLaCookieYDevuelve204() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ResponseEntity<Void> response = authController.logout(servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Cookie cookie = servletResponse.getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    void me_devuelve401SiNoHayToken() {
        ResponseEntity<AuthResponse> response = authController.me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_devuelve401SiElTokenEstaEnBlanco() {
        ResponseEntity<AuthResponse> response = authController.me("   ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_devuelve401SiElTokenNoEsValido() {
        when(jwtService.isTokenValid("token-invalido")).thenReturn(false);

        ResponseEntity<AuthResponse> response = authController.me("token-invalido");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_devuelve200ConLosDatosDelUsuario() {
        when(jwtService.isTokenValid("token-123")).thenReturn(true);
        when(authService.getMe("token-123")).thenReturn(new AuthResponse("juan", "juan@example.com", null));

        ResponseEntity<AuthResponse> response = authController.me("token-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("juan");
    }
}
