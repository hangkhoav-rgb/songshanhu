package com.songshanhu.blog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private Boolean mustResetPassword;
    private String resetToken;
    private UserResponse user;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserResponse {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String email;
        private String role;
    }
}
