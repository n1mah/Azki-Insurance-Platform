package com.azki.auth.controller;

import com.azki.auth.dto.AuthResponse;
import com.azki.auth.dto.LoginRequest;
import com.azki.auth.dto.RegisterRequest;
import com.azki.auth.entity.User;
import com.azki.auth.security.TokenIssuer;
import com.azki.auth.service.UserService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "User registration and login")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenIssuer tokenIssuer;

    public AuthController(UserService userService, TokenIssuer tokenIssuer) {
        this.userService = userService;
        this.tokenIssuer = tokenIssuer;
    }

    @Operation(summary = "Register a new user", description = "Creates a user with the CUSTOMER role and returns a JWT")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.password());
        String token = tokenIssuer.generateToken(user.getId().toString(), user.getUsername(), user.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
        }

    @Operation(summary = "Authenticate an existing user", description = "Returns a fresh JWT on successful login")
    @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.username(), request.password());
        String token = tokenIssuer.generateToken(user.getId().toString(), user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}