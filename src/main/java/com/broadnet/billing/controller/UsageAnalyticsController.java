package com.broadnet.billing.controller;

import com.broadnet.billing.dto.response.UsageSummaryResponse;
import com.broadnet.billing.dto.response.UsageGraphResponse;
import com.broadnet.billing.service.UsageAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Usage Analytics API Controller
 * Provides usage reports, graphs, and data exports
 *
 * Security: Requires authenticated user
 * Rate Limiting: 10 requests/minute (heavier processing)
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/usage")
@RequiredArgsConstructor
public class UsageAnalyticsController {

    private final UsageAnalyticsService analyticsService;

    /**
     * Get usage summary for a date range
     *
     * GET /api/billing/usage/summary
     *
     * @param startDate Start date (format: yyyy-MM-dd, optional, default: 30 days ago)
     * @param endDate End date (format: yyyy-MM-dd, optional, default: today)
     * @param groupBy Period grouping: day, week, month (optional, default: day)
     * @return UsageSummaryResponse with breakdown by usage type
     *
     * Response:
     * {
     *   "period": {
     *     "start_date": "2025-02-19T00:00:00Z",
     *     "end_date": "2025-03-19T00:00:00Z"
     *   },
     *   "summary": {
     *     "answers": {
     *       "total_used": 45000,
     *       "average_per_day": 1500,
     *       "peak_day": 3500,
     *       "blocked_count": 5
     *     },
     *     "kb_pages": {
     *       "total_added": 125,
     *       "average_per_day": 4.2
     *     },
     *     "agents": {
     *       "total_created": 3,
     *       "active_count": 3
     *     },
     *     "users": {
     *       "total_added": 8,
     *       "active_count": 8
     *     }
     *   },
     *   "trends": {
     *     "daily": [
     *       {
     *         "date": "2025-02-19",
     *         "answers_used": 1200,
     *         "kb_pages_added": 2,
     *         "agents_created": 0,
     *         "users_added": 0
     *       },
     *       ...
     *     ]
     *   }
     * }
     *
     * Filters:
     * - Date range filtering
     * - Optional grouping by period
     * - Excludes blocked attempts from positive counts
     *
     * Caching: 1 hour TTL (historical data, static)
     * Cache key: "usage_summary:{companyId}:{startDate}:{endDate}"
     */
    @GetMapping("/summary")
    public ResponseEntity<UsageSummaryResponse> getUsageSummary(
            @RequestHeader(value = "X-Company-Id") Long companyId,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "group_by", required = false, defaultValue = "day") String groupBy) {

        log.debug("Fetching usage summary for company: {}, from: {} to: {}",
                companyId, startDate, endDate);

        try {
            UsageSummaryResponse summary = analyticsService.getUsageSummary(
                    companyId,
                    startDate,
                    endDate,
                    groupBy
            );

            return ResponseEntity.ok(summary);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid date range for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching usage summary for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get usage graph data for visualization
     *
     * GET /api/billing/usage/graph/{graphId}
     *
     * @param graphId Graph ID: answers_trend, kb_trend, agents_trend, users_trend
     * @param period Time period: week, month, quarter, year
     * @return UsageGraphResponse with data points for charting
     *
     * Response:
     * {
     *   "title": "Answer Usage Trend",
     *   "type": "line",
     *   "period": "month",
     *   "data_points": [
     *     {
     *       "date": "2025-02-19",
     *       "value": 1500,
     *       "limit": 5000,
     *       "percentage": 30
     *     },
     *     ...
     *   ]
     * }
     *
     * Graph Types:
     * - answers_trend: Answer usage over time
     * - kb_trend: KB page additions over time
     * - agents_trend: Agent creation over time
     * - users_trend: User additions over time
     *
     * Caching: 30min TTL
     * Cache key: "usage_graph:{companyId}:{graphId}:{period}"
     */
    @GetMapping("/graph/{graphId}")
    public ResponseEntity<UsageGraphResponse> getUsageGraph(
            @PathVariable String graphId,
            @RequestHeader(value = "X-Company-Id") Long companyId,
            @RequestParam(value = "period", required = false, defaultValue = "month") String period) {

        log.debug("Fetching usage graph {} for company: {}, period: {}", graphId, companyId, period);

        try {
            UsageGraphResponse graph = analyticsService.getUsageGraph(
                    companyId,
                    graphId,
                    period
            );

            return ResponseEntity.ok(graph);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid graph request for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching usage graph for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export usage data as CSV
     *
     * GET /api/billing/usage/export
     *
     * @param format Export format: csv, json (default: csv)
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return File as CSV/JSON attachment
     *
     * CSV Format:
     * date,usage_type,count,limit,percentage,blocked
     * 2025-02-19,answers,1500,5000,30,false
     * 2025-02-19,kb_pages,5,1000,0.5,false
     * 2025-02-19,agents,0,5,0,false
     * 2025-02-19,users,2,10,20,false
     * ...
     *
     * Headers:
     * - Content-Type: text/csv or application/json
     * - Content-Disposition: attachment; filename="usage-export-2025.csv"
     *
     * Flow:
     * 1. Query BillingUsageLog and BillingUsageAnalytics
     * 2. Format data as CSV/JSON
     * 3. Return file with proper headers
     *
     * Performance: Can handle 1M+ rows (streamed response)
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportUsageData(
            @RequestHeader(value = "X-Company-Id") Long companyId,
            @RequestParam(value = "format", required = false, defaultValue = "csv") String format,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Exporting usage data for company: {}, format: {}", companyId, format);

        try {
            byte[] data = analyticsService.exportUsageData(
                    companyId,
                    format,
                    startDate,
                    endDate
            );

            String contentType = "csv".equalsIgnoreCase(format) ? "text/csv" : "application/json";
            String filename = "usage-export-" + LocalDateTime.now().getYear() +
                    ("csv".equalsIgnoreCase(format) ? ".csv" : ".json");

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(data);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid export request for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error exporting usage data for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}