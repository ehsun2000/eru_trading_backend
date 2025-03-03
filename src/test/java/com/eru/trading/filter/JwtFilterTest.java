package com.eru.trading.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.eru.trading.filter.JwtAuthenticationFilter;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Filter 測試")
class JwtFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtFilter = new JwtAuthenticationFilter();
    }

    @Test
    @DisplayName("當請求沒有 Authorization header 時應該直接通過")
    void should_pass_through_when_no_auth_header() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("當 Authorization header 不是以 Bearer 開頭時應該直接通過")
    void should_pass_through_when_not_bearer_token() {
    }

    @Test
    @DisplayName("當 token 有效時應該設置 Authentication")
    void should_set_authentication_when_token_is_valid() {
    }

    @Test
    @DisplayName("當 token 無效時應該不設置 Authentication")
    void should_not_set_authentication_when_token_is_invalid() {
    }

    @Test
    @DisplayName("當 token 過期時應該不設置 Authentication")
    void should_not_set_authentication_when_token_is_expired() {
    }

}