package com.songshanhu.blog.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(min = 2, max = 20, message = "昵称长度需为 2-20")
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\-·\\s]+$", message = "昵称包含非法字符")
    private String nickname;

    @Pattern(regexp = "^(male|female|secret)$", message = "性别取值非法")
    private String gender;

    @Size(max = 200, message = "个人简介最长 200 字")
    private String bio;

    @Size(max = 2000, message = "扩展字段过长")
    private String extra;
}

