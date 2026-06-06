package com.bloom.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated platform analytics for the ADMIN dashboard.
 * Computed on-demand from the users table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long deletedUsers;
    private long newUsersThisMonth;
    private long adminCount;
    private long studentCount;

    /** Sign-ups per day for the last 30 days (chronological). */
    private List<DailyCount> signupsByDay;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyCount {
        private String date;   // ISO yyyy-MM-dd
        private long count;
    }
}
