package com.broadnet.billing.service;


import com.broadnet.billing.dto.response.UsageGraphResponse;
import com.broadnet.billing.dto.response.UsageSummaryResponse;

import java.time.LocalDate;

/**
 * USAGE ANALYTICS SERVICE
 * Provides usage reports and analytics
 */
public interface UsageAnalyticsService {

    /**
     * Get usage summary for date range
     *
     * Aggregates:
     * - BillingUsageLog entries
     * - BillingUsageAnalytics pre-aggregated data
     *
     * Calculates:
     * - Total usage per type
     * - Average usage per day
     * - Peak day usage
     * - Blocked usage count
     * - Trends over time
     *
     * Filters:
     * - Date range (start_date to end_date)
     * - Optional grouping: day, week, month
     *
     * Return: UsageSummaryResponse with full breakdown
     */
    UsageSummaryResponse getUsageSummary(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate,
            String groupBy
    );

    /**
     * Get usage graph data for charting
     *
     * Graph types:
     * - answers_trend: Answer usage over time
     * - kb_trend: KB page additions
     * - agents_trend: Agent creation
     * - users_trend: User additions
     *
     * Includes:
     * - Data points with date
     * - Limit overlay (if applicable)
     * - Percentage of limit
     *
     * Periods:
     * - week: Last 7 days (daily points)
     * - month: Last 30 days (daily points)
     * - quarter: Last 90 days (weekly points)
     * - year: Last 365 days (monthly points)
     *
     * Return: UsageGraphResponse ready for charting library
     */
    UsageGraphResponse getUsageGraph(Long companyId, String graphId, String period);

    /**
     * Export usage data as CSV or JSON
     *
     * Format:
     * - CSV: date,usage_type,count,limit,percentage,blocked
     * - JSON: Array of usage records
     *
     * Filters:
     * - Date range
     * - Optional: usage type filter
     *
     * Performance: Streamed for large datasets (1M+ rows)
     *
     * Return: Byte array (CSV or JSON) for download
     */
    byte[] exportUsageData(
            Long companyId,
            String format,
            LocalDate startDate,
            LocalDate endDate
    );
}
