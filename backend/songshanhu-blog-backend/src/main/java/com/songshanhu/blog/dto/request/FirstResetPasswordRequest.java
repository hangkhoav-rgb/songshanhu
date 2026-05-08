package com.songshanhu.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FirstResetPasswordRequest {
    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 6, max = 32)
    private String newPassword;
}

