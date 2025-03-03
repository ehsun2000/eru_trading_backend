package com.eru.trading.filter;

import com.eru.trading.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Filter 測試")
class JwtFilterTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_USERNAME = "testUser";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtFilter = new JwtAuthenticationFilter(jwtTokenService, userDetailsService);
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
    void should_set_authentication_when_token_is_valid() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + TEST_TOKEN);
        when(jwtTokenService.parseToken(TEST_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtTokenService.validateToken(TEST_TOKEN, userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication instanceof UsernamePasswordAuthenticationToken);
        assertEquals(userDetails, authentication.getPrincipal());
        assertEquals(0, authentication.getAuthorities().size());
        assertNull(authentication.getCredentials());
    }

    @Test
    @DisplayName("當 token 無效時應該不設置 Authentication")
    void should_not_set_authentication_when_token_is_invalid() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + TEST_TOKEN);
        when(jwtTokenService.parseToken(TEST_TOKEN)).thenThrow(new JwtException("Invalid token"));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("當 token 過期時應該不設置 Authentication")
    void should_not_set_authentication_when_token_is_expired() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + TEST_TOKEN);
        when(jwtTokenService.parseToken(TEST_TOKEN)).thenThrow(
                new ExpiredJwtException(null, null, "Token has expired"));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

}