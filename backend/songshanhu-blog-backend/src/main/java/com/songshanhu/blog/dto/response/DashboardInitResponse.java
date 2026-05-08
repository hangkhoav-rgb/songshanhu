package com.songshanhu.blog.dto.response;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class DashboardInitResponse {
    private DashboardStatsResponse stats;
    private DashboardSummaryResponse summary;
    private Page<DashboardActionVO> actions;
}

