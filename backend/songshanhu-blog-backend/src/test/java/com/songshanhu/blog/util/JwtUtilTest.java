package com.songshanhu.blog.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void firstResetToken_containsPurpose() {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "test-test-test-test-test-test-test-test-test-test-test-test-test=");
        ReflectionTestUtils.setField(jwt, "expiration", 86400000L);
        ReflectionTestUtils.setField(jwt, "resetExpiration", 600000L);
        jwt.init();

        String token = jwt.generateFirstResetToken("u1");
        assertThat(jwt.extractUsername(token)).isEqualTo("u1");
        assertThat(jwt.extractPurpose(token)).isEqualTo("first_reset");
    }
}

