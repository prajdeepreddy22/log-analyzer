package com.loganalyzer.controller;

import com.loganalyzer.config.AppProperties;
import com.loganalyzer.dto.request.LoginRequest;
import com.loganalyzer.dto.request.RegisterRequest;
import com.loganalyzer.dto.request.UpdateProfileRequest;
import com.loganalyzer.dto.response.AuthResponse;
import com.loganalyzer.dto.response.UserProfileResponse;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.ConflictException;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.repository.UserRepository;
import com.loganalyzer.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AppProperties appProperties;

    private Long extractUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userId;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        log.info("Register request for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw resolveRegistrationConflict(request, exception);
        }

        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        java.util.List.of(new org.springframework.security.core.authority
                                .SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

        String token = jwtService.generateToken(userDetails, user.getId());

        return ResponseEntity.status(201).body(
                AuthResponse.builder()
                        .token(token)
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .expiresIn(appProperties.getJwt().getExpiration())
                        .message("Registration successful")
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        log.info("Login request for username: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = jwtService.generateToken(userDetails, user.getId());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .expiresIn(appProperties.getJwt().getExpiration())
                        .message("Login successful")
                        .build()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            HttpServletRequest request) {

        Long userId = extractUserId(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(
                toProfileResponse(user, "Profile fetched successfully")
        );
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest servletRequest) {

        Long userId = extractUserId(servletRequest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setDisplayName(request.getDisplayName().trim());

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(
                toProfileResponse(savedUser, "Profile updated successfully")
        );
    }

    private UserProfileResponse toProfileResponse(
            User user,
            String message
    ) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message(message)
                .build();
    }

    private ConflictException resolveRegistrationConflict(
            RegisterRequest request,
            DataIntegrityViolationException exception
    ) {

        String databaseMessage = exception.getMostSpecificCause().getMessage();
        String normalizedMessage = databaseMessage == null
                ? ""
                : databaseMessage.toLowerCase();

        if (normalizedMessage.contains("username")) {
            return new ConflictException("Username already exists");
        }

        if (normalizedMessage.contains("email")) {
            return new ConflictException("Email already exists");
        }

        log.warn(
                "Registration data conflict username={} type={}",
                request.getUsername(),
                exception.getClass().getSimpleName()
        );

        return new ConflictException(
                "Registration conflicts with an existing account"
        );
    }
}
