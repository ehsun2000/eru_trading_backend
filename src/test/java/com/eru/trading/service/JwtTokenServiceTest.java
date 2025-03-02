package com.eru.trading.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JWT Token 服務測試")
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private UserDetails userDetails;
    private static final String SECRET_KEY = "8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb";
    private static final long EXPIRATION_TIME = 3600000L; // 1 hour

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

    @Test
    @DisplayName("產生 Token 時應包含正確的聲明")
    void should_generate_token_with_correct_claims() {
        // Arrange
        String username = "testUser";
        when(userDetails.getUsername()).thenReturn(username);

        // Act
        String token = jwtTokenService.generateToken(userDetails);

        // Assert
        assertThat(token).isNotNull();
        Claims claims = jwtTokenService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration())
                .isNotNull()
                .isAfter(new Date());
    }

    @ParameterizedTest(name = "測試不同用戶 {0} 和 {1} 產生的 Token 應不同")
    @CsvSource({
            "user1, user2",
            "admin, user",
            "test1, test2"
    })
    void should_generate_different_tokens_for_different_users(String firstUser, String secondUser) {
        // Arrange
        UserDetails user1 = mock(UserDetails.class);
        UserDetails user2 = mock(UserDetails.class);
        when(user1.getUsername()).thenReturn(firstUser);
        when(user2.getUsername()).thenReturn(secondUser);

        // Act
        String token1 = jwtTokenService.generateToken(user1);
        String token2 = jwtTokenService.generateToken(user2);

        // Assert
        assertThat(token1)
                .isNotNull()
                .isNotEqualTo(token2);

        Claims claims1 = jwtTokenService.parseToken(token1);
        Claims claims2 = jwtTokenService.parseToken(token2);

        assertThat(claims1.getSubject()).isEqualTo(firstUser);
        assertThat(claims2.getSubject()).isEqualTo(secondUser);
    }

    @Test
    @DisplayName("Token 應包含正確的過期時間")
    void should_include_expiration_time_in_token() {
        // Arrange
        long currentTimeMillis = System.currentTimeMillis();

        // Act
        String token = jwtTokenService.generateToken(userDetails);

        // Assert
        Claims claims = jwtTokenService.parseToken(token);
        Date expirationDate = claims.getExpiration();
        assertThat(expirationDate).isNotNull();

        long expectedExpirationTime = currentTimeMillis + EXPIRATION_TIME;
        long actualExpirationTime = expirationDate.getTime();
        long timeDifference = Math.abs(expectedExpirationTime - actualExpirationTime);

        assertThat(timeDifference)
                .as("過期時間應在預期時間的1秒誤差範圍內")
                .isLessThan(1000);

        assertThat(expirationDate)
                .isAfter(new Date(currentTimeMillis))
                .isBefore(new Date(currentTimeMillis + EXPIRATION_TIME + 1000));
    }

    @ParameterizedTest(name = "測試無效的 Token 格式：{0}")
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "invalid.token", "malformed.jwt.token" })
    void should_reject_invalid_tokens(String invalidToken) {
        assertThrows(Exception.class, () -> {
            jwtTokenService.parseToken(invalidToken);
        }, String.format("無效的 Token 格式：%s 應該拋出異常", invalidToken));
    }

    @Test
    @DisplayName("過期的 Token 應拋出異常")
    void should_reject_expired_token() throws Exception {
        // Arrange - Set a very short expiration time for testing
        long originalExpiration = EXPIRATION_TIME;
        try {
            var expirationField = JwtTokenService.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtTokenService, 1000L); // Set to 1 second

            String token = jwtTokenService.generateToken(userDetails);
            assertThat(jwtTokenService.parseToken(token).getExpiration()).isAfter(new Date());

            // Act & Assert
            Thread.sleep(1100); // Wait just over 1 second
            assertThrows(ExpiredJwtException.class, () -> {
                jwtTokenService.parseToken(token);
            }, "過期的 Token 應拋出 ExpiredJwtException");

        } finally {
            var expirationField = JwtTokenService.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtTokenService, originalExpiration);
        }
    }
}