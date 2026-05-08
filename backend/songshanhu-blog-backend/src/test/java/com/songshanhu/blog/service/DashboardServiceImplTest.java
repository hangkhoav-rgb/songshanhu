package com.songshanhu.blog.service;

import com.songshanhu.blog.dto.response.DashboardSummaryResponse;
import com.songshanhu.blog.mapper.DashboardMapper;
import com.songshanhu.blog.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceImplTest {

    @Test
    void getSummary_cacheAside_works() {
        DashboardMapper mapper = Mockito.mock(DashboardMapper.class);
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hash = Mockito.mock(HashOperations.class);

        Mockito.when(redis.opsForHash()).thenReturn(hash);
        Mockito.when(hash.get(Mockito.anyString(), Mockito.eq("likeCount"))).thenReturn(null);
        Mockito.when(hash.get(Mockito.anyString(), Mockito.eq("collectCount"))).thenReturn(null);
        Mockito.when(hash.get(Mockito.anyString(), Mockito.eq("lastActionTime"))).thenReturn(null);

        DashboardSummaryResponse db = new DashboardSummaryResponse();
        db.setLikeCount(3);
        db.setCollectCount(5);
        db.setLastActionTime(LocalDateTime.of(2026, 4, 6, 12, 0));
        Mockito.when(mapper.selectSummary(1L)).thenReturn(db);

        DashboardServiceImpl svc = new DashboardServiceImpl(mapper, redis);
        DashboardSummaryResponse out = svc.getSummary(1L);

        assertThat(out.getLikeCount()).isEqualTo(3);
        assertThat(out.getCollectCount()).isEqualTo(5);
        assertThat(out.getLastActionTime()).isNotNull();

        Mockito.verify(redis, Mockito.atLeastOnce()).opsForHash();
        Mockito.verify(mapper).selectSummary(1L);
        Mockito.verify(hash).put(Mockito.anyString(), Mockito.eq("likeCount"), Mockito.eq("3"));
        Mockito.verify(hash).put(Mockito.anyString(), Mockito.eq("collectCount"), Mockito.eq("5"));
    }
}

