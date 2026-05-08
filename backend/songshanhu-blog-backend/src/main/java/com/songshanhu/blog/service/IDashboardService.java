package com.songshanhu.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.dto.response.DashboardActionVO;
import com.songshanhu.blog.dto.response.DashboardSummaryResponse;

public interface IDashboardService {
    DashboardSummaryResponse getSummary(Long userId);

    Page<DashboardActionVO> getRecentActions(Long userId, int current, int size, String actionType, int days);

    void invalidateUserSummaryCache(Long userId);
}

