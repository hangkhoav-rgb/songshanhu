package com.songshanhu.blog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.dto.response.DashboardActionVO;
import com.songshanhu.blog.dto.response.DashboardSummaryResponse;
import com.songshanhu.blog.mapper.DashboardMapper;
import com.songshanhu.blog.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final Duration SUMMARY_TTL = Duration.ofMinutes(5);

    private final DashboardMapper dashboardMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public DashboardSummaryResponse getSummary(Long userId) {
        String key = summaryKey(userId);

        String like = redisTemplate.opsForHash().get(key, "likeCount") instanceof String s ? s : null;
        String collect = redisTemplate.opsForHash().get(key, "collectCount") instanceof String s ? s : null;
        String last = redisTemplate.opsForHash().get(key, "lastActionTime") instanceof String s ? s : null;

        if (like != null && collect != null && last != null) {
            DashboardSummaryResponse resp = new DashboardSummaryResponse();
            resp.setLikeCount(Long.parseLong(like));
            resp.setCollectCount(Long.parseLong(collect));
            resp.setLastActionTime(last.isBlank() ? null : java.time.LocalDateTime.parse(last));
            return resp;
        }

        DashboardSummaryResponse db = dashboardMapper.selectSummary(userId);
        if (db == null) {
            db = new DashboardSummaryResponse();
        }

        redisTemplate.opsForHash().put(key, "likeCount", String.valueOf(db.getLikeCount()));
        redisTemplate.opsForHash().put(key, "collectCount", String.valueOf(db.getCollectCount()));
        redisTemplate.opsForHash().put(key, "lastActionTime", db.getLastActionTime() == null ? "" : db.getLastActionTime().toString());
        redisTemplate.expire(key, SUMMARY_TTL);
        return db;
    }

    @Override
    public Page<DashboardActionVO> getRecentActions(Long userId, int current, int size, String actionType, int days) {
        Page<DashboardActionVO> page = new Page<>(current, size);
        return dashboardMapper.selectRecentActions(page, userId, actionType, days);
    }

    @Override
    public void invalidateUserSummaryCache(Long userId) {
        redisTemplate.delete(summaryKey(userId));
    }

    private String summaryKey(Long userId) {
        return "dashboard:summary:" + userId;
    }
}

