package com.speedsneakers.userservice.service;

import com.speedsneakers.userservice.exception.UserAlreadyExistsException;
import com.speedsneakers.userservice.model.entity.Role;
import com.speedsneakers.userservice.model.entity.User;
import com.speedsneakers.userservice.model.request.LoginRequest;
import com.speedsneakers.userservice.model.request.RegisterRequest;
import com.speedsneakers.userservice.model.response.AuthResponse;
import com.speedsneakers.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, passwordEncoder);
    }

    @Test
    void register_creaUsuarioYDevuelveToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("juan");
        request.setEmail("juan@example.com");
        request.setPassword("secreto1");

        when(userRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("juan")).thenReturn(false);
        when(passwordEncoder.encode("secreto1")).thenReturn("hash-secreto1");
        when(jwtService.generateToken(any(User.class))).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        assertThat(response.getUsername()).isEqualTo("juan");
        assertThat(response.getEmail()).isEqualTo("juan@example.com");
        assertThat(response.getToken()).isEqualTo("token-123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("hash-secreto1");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void register_lanzaExcepcionSiElEmailYaExiste() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("juan");
        request.setEmail("juan@example.com");
        request.setPassword("secreto1");

        when(userRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("El email ya está registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_lanzaExcepcionSiElUsernameYaExiste() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("juan");
        request.setEmail("juan@example.com");
        request.setPassword("secreto1");

        when(userRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("juan")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("El nombre de usuario ya está en uso");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_devuelveTokenConCredencialesValidas() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@example.com");
        request.setPassword("secreto1");

        User user = new User();
        user.setId(1L);
        user.setUsername("juan");
        user.setEmail("juan@example.com");
        user.setPassword("hash-secreto1");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secreto1", "hash-secreto1")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-123");

        AuthResponse response = authService.login(request);

        assertThat(response.getUsername()).isEqualTo("juan");
        assertThat(response.getToken()).isEqualTo("token-123");
    }

    @Test
    void login_lanzaExcepcionSiElUsuarioNoExiste() {
        LoginRequest request = new LoginRequest();
        request.setEmail("desconocido@example.com");
        request.setPassword("secreto1");

        when(userRepository.findByEmail("desconocido@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_lanzaExcepcionSiLaContrasenaNoCoincide() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@example.com");
        request.setPassword("incorrecta");

        User user = new User();
        user.setId(1L);
        user.setEmail("juan@example.com");
        user.setPassword("hash-secreto1");

        when(userRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrecta", "hash-secreto1")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void getMe_devuelveUsuarioSinToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("juan");
        user.setEmail("juan@example.com");

        when(jwtService.extractUserId("token-123")).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthResponse response = authService.getMe("token-123");

        assertThat(response.getUsername()).isEqualTo("juan");
        assertThat(response.getEmail()).isEqualTo("juan@example.com");
        assertThat(response.getToken()).isNull();
    }

    @Test
    void getMe_lanzaExcepcionSiElUsuarioNoExiste() {
        when(jwtService.extractUserId("token-123")).thenReturn("99");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe("token-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }
}
