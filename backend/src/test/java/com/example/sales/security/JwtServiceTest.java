package com.example.sales.security;

import com.example.sales.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Base64 encoded secret key (minimum 256 bits for HS256)
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "c2FsZXMtYnJhaW4tc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seS1jaGFuZ2UtaW4tcHJvZHVjdGlvbg==");
        ReflectionTestUtils.setField(jwtService, "expirationHours", 24L);

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("generateToken - should create valid JWT token")
    void generateToken_Success() {
        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("extractEmail - should extract email from token")
    void extractEmail_Success() {
        // Given
        String token = jwtService.generateToken(user);

        // When
        String email = jwtService.extractEmail(token);

        // Then
        assertThat(email).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("isTokenValid - should return true for valid token")
    void isTokenValid_ValidToken() {
        // Given
        String token = jwtService.generateToken(user);

        // When
        boolean isValid = jwtService.isTokenValid(token, user);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - should return false for different user")
    void isTokenValid_DifferentUser() {
        // Given
        String token = jwtService.generateToken(user);
        User differentUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .password("encodedPassword")
                .name("Other User")
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("getExpirationSeconds - should return correct expiration time")
    void getExpirationSeconds_Success() {
        // When
        long expirationSeconds = jwtService.getExpirationSeconds();

        // Then
        assertThat(expirationSeconds).isEqualTo(24 * 60 * 60); // 24 hours in seconds
    }
}
