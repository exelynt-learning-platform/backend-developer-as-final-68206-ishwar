package com.example.booking.service;

import com.example.booking.dto.*;
import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + authentication.getName()));

        String token = jwtService.generateToken(
                user.getUsername(), user.getRole().name());

        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = userRepository.save(new User(
                username,
                passwordEncoder.encode(request.password()),
                Role.USER
        ));

        String token = jwtService.generateToken(
                user.getUsername(), user.getRole().name());

        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}
