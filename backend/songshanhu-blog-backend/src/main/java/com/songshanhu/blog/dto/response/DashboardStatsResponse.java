package com.songshanhu.blog.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsResponse {
    private Long totalViews;
    private Long totalLikes;
    private Long totalComments;
    private Long totalArticles;
    
    private List<CategoryStats> categoryDistribution;
    private List<DailyStats> dailyTrend;
    
    private List<LocationStats> locationPoints;
    
    @Data
    public static class CategoryStats {
        private String name;
        private Long value;
    }
    
    @Data
    public static class DailyStats {
        private String date;
        private Long count;
    }
    
    @Data
    public static class LocationStats {
        private Double longitude;
        private Double latitude;
        private String title;
    }
}
