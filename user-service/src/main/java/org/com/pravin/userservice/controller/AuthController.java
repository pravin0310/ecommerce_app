package org.com.pravin.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.pravin.userservice.dto.AuthResponse;
import org.com.pravin.userservice.dto.LoginRequest;
import org.com.pravin.userservice.dto.RegisterRequest;
import org.com.pravin.userservice.entity.User;
import org.com.pravin.userservice.repository.UserRepository;
import org.com.pravin.userservice.service.AuthService;
import org.com.pravin.userservice.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request);
        User user = userRepository.findByEmail(request.getEmail()).get();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getEmail(), user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        User user = userRepository.findByEmail(request.getEmail()).get();
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getId()));
    }
}
