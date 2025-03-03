package com.eru.trading.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JWT Filter 測試")
class JwtFilterTest {

    @Test
    @DisplayName("當請求沒有 Authorization header 時應該直接通過")
    void should_pass_through_when_no_auth_header() {
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