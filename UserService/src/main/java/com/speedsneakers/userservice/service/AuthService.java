package com.speedsneakers.userservice.service;

import com.speedsneakers.userservice.exception.UserAlreadyExistsException;
import com.speedsneakers.userservice.model.entity.Role;
import com.speedsneakers.userservice.model.entity.User;
import com.speedsneakers.userservice.model.request.LoginRequest;
import com.speedsneakers.userservice.model.request.RegisterRequest;
import com.speedsneakers.userservice.model.response.AuthResponse;
import com.speedsneakers.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("El email ya está registrado");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está en uso");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Usuario registrado: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        return new AuthResponse(user.getUsername(), user.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        log.info("Usuario autenticado: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        return new AuthResponse(user.getUsername(), user.getEmail(), token);
    }

    public AuthResponse getMe(String token) {
        String userId = jwtService.extractUserId(token);
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return new AuthResponse(user.getUsername(), user.getEmail(), null);
    }
}
