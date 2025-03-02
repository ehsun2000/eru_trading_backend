package com.eru.trading.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JWT Token Service Tests")
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private UserDetails userDetails;
    private static final String SECRET_KEY = "8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb";
    private static final long EXPIRATION_TIME = 3600000L; // 1 hour

    /**
     * 解析 JWT token 並返回其聲明
     * 
     * @param token JWT token 字符串
     * @return token 中的聲明
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService();
        try {
            var secretField = JwtTokenService.class.getDeclaredField("secret");
            var expirationField = JwtTokenService.class.getDeclaredField("expiration");
            secretField.setAccessible(true);
            expirationField.setAccessible(true);
            secretField.set(jwtTokenService, SECRET_KEY);
            expirationField.set(jwtTokenService, EXPIRATION_TIME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testUser");
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate token with correct claims")
        void should_generate_token_with_correct_claims() {
            // Arrange
            String username = "testUser";
            when(userDetails.getUsername()).thenReturn(username);

            // Act
            String token = jwtTokenService.generateToken(userDetails);

            // Assert
            assertThat(token).isNotNull();
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo(username);
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration())
                    .isNotNull()
                    .isAfter(new Date());
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void should_generate_different_tokens_for_different_users() {
            // Arrange
            UserDetails user1 = mock(UserDetails.class);
            UserDetails user2 = mock(UserDetails.class);
            when(user1.getUsername()).thenReturn("user1");
            when(user2.getUsername()).thenReturn("user2");

            // Act
            String token1 = jwtTokenService.generateToken(user1);
            String token2 = jwtTokenService.generateToken(user2);

            // Assert
            assertThat(token1)
                    .isNotNull()
                    .isNotEqualTo(token2);

            // Verify claims
            Claims claims1 = parseToken(token1);
            Claims claims2 = parseToken(token2);

            assertThat(claims1.getSubject()).isEqualTo("user1");
            assertThat(claims2.getSubject()).isEqualTo("user2");
            assertThat(claims1.getSubject()).isNotEqualTo(claims2.getSubject());
        }

        @Test
        @DisplayName("Should include expiration time in token")
        void should_include_expiration_time_in_token() {
            // Arrange
            long currentTimeMillis = System.currentTimeMillis();

            // Act
            String token = jwtTokenService.generateToken(userDetails);

            // Assert
            Claims claims = parseToken(token);
            Date expirationDate = claims.getExpiration();
            assertThat(expirationDate).isNotNull();

            // 驗證過期時間是否在當前時間的一小時後（允許1秒誤差）
            long expectedExpirationTime = currentTimeMillis + EXPIRATION_TIME;
            long actualExpirationTime = expirationDate.getTime();
            long timeDifference = Math.abs(expectedExpirationTime - actualExpirationTime);

            assertThat(timeDifference)
                    .as("Expiration time should be within 1 second of expected time")
                    .isLessThan(1000); // 允許1秒誤差

            // 驗證過期時間是否在將來
            assertThat(expirationDate)
                    .isAfter(new Date(currentTimeMillis))
                    .isBefore(new Date(currentTimeMillis + EXPIRATION_TIME + 1000));
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token")
        void should_validate_valid_token() {
            // Arrange
            String token = jwtTokenService.generateToken(userDetails);

            // Act
            boolean isValid = jwtTokenService.validateToken(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should generate expired token exception")
        void should_generate_expired_token_exception() throws Exception {
            // Arrange - Set a very short expiration time for testing
            long originalExpiration = EXPIRATION_TIME;
            try {
                var expirationField = JwtTokenService.class.getDeclaredField("expiration");
                expirationField.setAccessible(true);
                expirationField.set(jwtTokenService, 1000L); // Set to 1 second

                String token = jwtTokenService.generateToken(userDetails);

                // Act & Assert
                Thread.sleep(1100); // Wait just over 1 second
                assertThrows(ExpiredJwtException.class, () -> {
                    parseToken(token);
                }, "Token parsing should fail with ExpiredJwtException");

            } finally {
                // Restore original expiration time
                var expirationField = JwtTokenService.class.getDeclaredField("expiration");
                expirationField.setAccessible(true);
                expirationField.set(jwtTokenService, originalExpiration);
            }
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void should_reject_token_with_invalid_signature() {
            // Arrange
            String token = jwtTokenService.generateToken(userDetails);
            String wrongKey = "different-secret-key-that-is-also-at-least-256-bits-long";

            // Act & Assert
            assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> {
                Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(wrongKey.getBytes()))
                        .build()
                        .parseSignedClaims(token);
            }, "Token should be rejected when verified with wrong key");
        }

        @Test
        @DisplayName("Should reject malformed token")
        void should_reject_malformed_token() {
            // Arrange
            String malformedToken = "invalid.jwt.token";

            // Act & Assert
            assertThrows(io.jsonwebtoken.MalformedJwtException.class, () -> {
                parseToken(malformedToken);
            }, "Should throw MalformedJwtException for invalid token format");
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void should_extract_username_from_valid_token() {
            // Arrange
            String expectedUsername = "testUser";
            when(userDetails.getUsername()).thenReturn(expectedUsername);
            String token = jwtTokenService.generateToken(userDetails);

            // Act
            Claims claims = parseToken(token);

            // Assert
            assertThat(claims.getSubject())
                    .as("Extracted username should match the original username")
                    .isEqualTo(expectedUsername);
        }

        @Test
        @DisplayName("Should extract expiration time from valid token")
        void should_extract_expiration_from_valid_token() {
            // TODO: Implement test
        }

        @Test
        @DisplayName("Should handle null or empty token")
        void should_handle_null_or_empty_token() {
            // TODO: Implement test
        }
    }
}