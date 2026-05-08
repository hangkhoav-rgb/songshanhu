package com.songshanhu.blog.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.dto.response.DashboardActionVO;
import com.songshanhu.blog.dto.response.DashboardSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardMapper {
    DashboardSummaryResponse selectSummary(@Param("userId") Long userId);

    Page<DashboardActionVO> selectRecentActions(Page<DashboardActionVO> page,
                                               @Param("userId") Long userId,
                                               @Param("actionType") String actionType,
                                               @Param("days") int days);
}

