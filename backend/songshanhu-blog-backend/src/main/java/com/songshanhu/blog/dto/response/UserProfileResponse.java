package com.songshanhu.blog.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String gender;
    private String bio;
    private String extra;
    private Map<String, String> avatarUrls;
}

