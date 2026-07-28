package com.philmed.auth;

import com.philmed.config.SecurityConfig;
import com.philmed.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Registration and login: request/response shapes, the service that does the
 * work, and the controller that exposes it — one file for the whole flow.
 */
public class Auth {

    public static class RegisterRequest {
        @NotBlank(message = "First name is required")
        public String firstName;

        @NotBlank(message = "Last name is required")
        public String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        public String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        public String password;

        @NotBlank(message = "Phone number is required")
        public String phone;
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        public String email;

        @NotBlank(message = "Password is required")
        public String password;
    }

    public static class AuthResponse {
        public String token;
        public String email;
        public String firstName;

        public AuthResponse(String token, String email, String firstName) {
            this.token = token;
            this.email = email;
            this.firstName = firstName;
        }
    }

    @org.springframework.stereotype.Service
    public static class Service {
        private final User.Repository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final SecurityConfig.JwtUtil jwtUtil;

        @Autowired
        public Service(User.Repository userRepository, PasswordEncoder passwordEncoder, SecurityConfig.JwtUtil jwtUtil) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtUtil = jwtUtil;
        }

        public AuthResponse register(RegisterRequest request) {
            if (userRepository.existsByEmail(request.email)) {
                throw new IllegalArgumentException("An account with this email already exists.");
            }
            User user = new User(request.firstName, request.lastName, request.email,
                    passwordEncoder.encode(request.password), request.phone);
            userRepository.save(user);
            String token = jwtUtil.generateToken(user.getEmail());
            return new AuthResponse(token, user.getEmail(), user.getFirstName());
        }

        public AuthResponse login(LoginRequest request) {
            User user = userRepository.findByEmail(request.email)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
            if (!passwordEncoder.matches(request.password, user.getPassword())) {
                throw new IllegalArgumentException("Invalid email or password.");
            }
            String token = jwtUtil.generateToken(user.getEmail());
            return new AuthResponse(token, user.getEmail(), user.getFirstName());
        }
    }

    @RestController
    @RequestMapping("/api/auth")
    public static class Controller {
        private final Service service;

        @Autowired
        public Controller(Service service) {
            this.service = service;
        }

        @PostMapping("/register")
        public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
            try {
                return ResponseEntity.ok(service.register(request));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        @PostMapping("/login")
        public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
            try {
                return ResponseEntity.ok(service.login(request));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(401).body(e.getMessage());
            }
        }
    }
}
