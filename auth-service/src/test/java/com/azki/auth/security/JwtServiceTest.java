package com.azki.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private static final long TEST_EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, TEST_EXPIRATION_MS);
    }

@Test
void shouldGenerateTokenContainingUserIdUsernameAndRole() {
    // given
    String userId = "user-id-123";
    String username = "nima_test";
    String role = "CUSTOMER";

    // when
    String token = jwtService.generateToken(userId, username, role);

    // then
    assertThat(token).isNotBlank();
    assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    assertThat(jwtService.extractUsername(token)).isEqualTo(username);
    assertThat(jwtService.extractRole(token)).isEqualTo(role);
}

    @Test
    void shouldValidateFreshlyGeneratedToken() {
        // given
        String token = jwtService.generateToken("user-id-123", "nima_test", "CUSTOMER");

        // when
        boolean isValid = jwtService.isTokenValid(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectTamperedToken() {
        // given
        String token = jwtService.generateToken("user-id-123", "nima_test", "CUSTOMER");
        String tamperedToken = token.substring(0, token.length() - 5) + "AAAAA";

        // when
        boolean isValid = jwtService.isTokenValid(tamperedToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        // given
        String malformedToken = "this.is.not.a.valid.jwt";

        // when
        boolean isValid = jwtService.isTokenValid(malformedToken);

        // then
        assertThat(isValid).isFalse();
    }

}