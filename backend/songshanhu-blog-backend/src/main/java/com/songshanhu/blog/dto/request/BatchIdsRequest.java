package com.songshanhu.blog.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchIdsRequest {
    @NotEmpty
    private List<Long> articleIds;
}

