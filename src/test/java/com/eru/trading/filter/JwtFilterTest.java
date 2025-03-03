package com.eru.trading.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final String AUTHORIZATION_HEADER = "Authorization";

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

    @ParameterizedTest(name = "當 Authorization header 為 {0} 時應該直接通過")
    @NullAndEmptySource
    @ValueSource(strings = {
            "Basic dXNlcjpwYXNzd29yZA==",
            "Digest xyz",
            "OAuth abc"
    })
    void should_pass_through_when_not_bearer_token(String headerValue) throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(headerValue);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
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