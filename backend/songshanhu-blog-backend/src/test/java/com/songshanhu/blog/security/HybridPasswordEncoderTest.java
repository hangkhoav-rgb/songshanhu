package com.songshanhu.blog.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HybridPasswordEncoderTest {

    @Test
    void matches_plainTextLegacy() {
        HybridPasswordEncoder enc = new HybridPasswordEncoder();
        assertThat(enc.matches("123", "123")).isTrue();
        assertThat(enc.matches("123", "124")).isFalse();
    }

    @Test
    void matches_bcrypt() {
        HybridPasswordEncoder enc = new HybridPasswordEncoder();
        String hash = enc.encode("Test12345!");
        assertThat(enc.matches("Test12345!", hash)).isTrue();
        assertThat(enc.matches("bad", hash)).isFalse();
    }
}

