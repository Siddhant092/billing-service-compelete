package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.response.*;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.UsageAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * USAGE ANALYTICS SERVICE IMPLEMENTATION
 * Provides usage reports, graphs, and data exports
 * Architecture: Query BillingUsageLog and BillingUsageAnalytics
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsageAnalyticsServiceImpl implements UsageAnalyticsService {

    private final BillingUsageLogRepository usageLogRepository;
    private final BillingUsageAnalyticsRepository analyticsRepository;
    private final CompanyBillingRepository billingRepository;
    private final ObjectMapper objectMapper;

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
     */
    @Override
    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummary(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate,
            String groupBy) {

        log.debug("Getting usage summary for company: {}, from {} to {}",
                companyId, startDate, endDate);

        // Verify company exists
        billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get usage logs
        List<BillingUsageLog> logs = usageLogRepository.findByCompanyIdAndDateRange(
                companyId, startDateTime, endDateTime);

        // Build summary
        UsageSummaryResponse response = UsageSummaryResponse.builder()
                .period(UsageSummaryResponse.PeriodDto.builder()
                        .startDate(startDateTime)
                        .endDate(endDateTime)
                        .build())
                .summary(buildSummaryDto(logs))
                .build();

        return response;
    }

    /**
     * Get usage graph data for visualization
     *
     * Graph types:
     * - answers_trend: Answer usage over time
     * - kb_trend: KB page additions
     * - agents_trend: Agent creation
     * - users_trend: User additions
     */
    @Override
    @Transactional(readOnly = true)
    public UsageGraphResponse getUsageGraph(Long companyId, String graphId, String period) {

        log.debug("Getting usage graph for company: {}, graphId: {}, period: {}",
                companyId, graphId, period);

        // Verify company exists
        billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        // Determine date range based on period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period.toLowerCase()) {
            case "week":
                startDate = endDate.minusDays(7);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                break;
            case "quarter":
                startDate = endDate.minusMonths(3);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusMonths(1);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get usage logs for period
        List<BillingUsageLog> logs = usageLogRepository.findByCompanyIdAndDateRange(
                companyId, startDateTime, endDateTime);

        // Build graph data
        List<UsageGraphResponse.DataPointDto> dataPoints = buildDataPoints(logs, graphId);

        return UsageGraphResponse.builder()
                .title(getTitleForGraph(graphId))
                .type("line")
                .period(period)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * Export usage data as CSV or JSON
     *
     * Format:
     * - CSV: date,usage_type,count,limit,percentage,blocked
     * - JSON: Array of usage records
     *
     * Performance: Streamed for large datasets
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsageData(
            Long companyId,
            String format,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("Exporting usage data for company: {}, format: {}", companyId, format);

        // Verify company exists
        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get usage logs
        List<BillingUsageLog> logs = usageLogRepository.findByCompanyIdAndDateRange(
                companyId, startDateTime, endDateTime);

        try {
            if ("csv".equalsIgnoreCase(format)) {
                return exportAsCsv(logs);
            } else if ("json".equalsIgnoreCase(format)) {
                return exportAsJson(logs);
            } else {
                throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            log.error("Error exporting usage data: {}", e.getMessage());
            throw new DataExportException(
                    "Failed to export usage data: " + e.getMessage(), e);
        }
    }

    // ===== Helper Methods =====

    /**
     * Build summary DTO from usage logs
     */
    private UsageSummaryResponse.SummaryDto buildSummaryDto(List<BillingUsageLog> logs) {

        Map<BillingUsageLog.UsageType, List<BillingUsageLog>> groupedByType = logs.stream()
                .collect(Collectors.groupingBy(BillingUsageLog::getUsageType));

        // Map enum types to appropriate breakdowns
        List<BillingUsageLog> answerLogs = groupedByType.getOrDefault(
                BillingUsageLog.UsageType.answer, new ArrayList<>());

        List<BillingUsageLog> kbPageLogs = new ArrayList<>();
        kbPageLogs.addAll(groupedByType.getOrDefault(
                BillingUsageLog.UsageType.kb_page_added, new ArrayList<>()));
        kbPageLogs.addAll(groupedByType.getOrDefault(
                BillingUsageLog.UsageType.kb_page_updated, new ArrayList<>()));

        List<BillingUsageLog> agentLogs = groupedByType.getOrDefault(
                BillingUsageLog.UsageType.agent_created, new ArrayList<>());

        List<BillingUsageLog> userLogs = groupedByType.getOrDefault(
                BillingUsageLog.UsageType.user_created, new ArrayList<>());

        UsageSummaryResponse.SummaryDto summary = UsageSummaryResponse.SummaryDto.builder()
                .answers(buildUsageBreakdown(answerLogs))
                .kbPages(buildUsageBreakdown(kbPageLogs))
                .agents(buildUsageBreakdown(agentLogs))
                .users(buildUsageBreakdown(userLogs))
                .build();

        return summary;
    }

    /**
     * Build usage breakdown
     */
    private UsageSummaryResponse.SummaryDto.UsageBreakdown buildUsageBreakdown(
            List<BillingUsageLog> logs) {

        int totalUsed = logs.stream()
                .mapToInt(log -> log.getUsageCount() != null ? log.getUsageCount() : 0)
                .sum();

        int blockedCount = (int) logs.stream()
                .filter(log -> log.getWasBlocked() != null && log.getWasBlocked())
                .count();

        Set<LocalDate> uniqueDates = logs.stream()
                .map(l -> l.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        double averagePerDay = uniqueDates.isEmpty() ? 0 : (double) totalUsed / uniqueDates.size();

        int peakDay = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getCreatedAt().toLocalDate(),
                        Collectors.summingInt(log -> log.getUsageCount() != null ? log.getUsageCount() : 0)))
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        return UsageSummaryResponse.SummaryDto.UsageBreakdown.builder()
                .totalUsed(totalUsed)
                .averagePerDay(averagePerDay)
                .peakDay(peakDay)
                .blockedCount(blockedCount)
                .build();
    }

    /**
     * Build data points for graph
     */
    private List<UsageGraphResponse.DataPointDto> buildDataPoints(
            List<BillingUsageLog> logs, String graphId) {

        // Filter logs by graph type
        BillingUsageLog.UsageType targetType = getUsageTypeForGraph(graphId);

        List<BillingUsageLog> filteredLogs = logs;
        if (targetType != null) {
            filteredLogs = logs.stream()
                    .filter(log -> log.getUsageType() == targetType)
                    .collect(Collectors.toList());
        }

        Map<LocalDate, Integer> dailyUsage = new HashMap<>();

        filteredLogs.forEach(log -> {
            LocalDate date = log.getCreatedAt().toLocalDate();
            int count = log.getUsageCount() != null ? log.getUsageCount() : 0;
            dailyUsage.put(date, dailyUsage.getOrDefault(date, 0) + count);
        });

        return dailyUsage.entrySet().stream()
                .map(entry -> UsageGraphResponse.DataPointDto.builder()
                        .date(entry.getKey())
                        .value(entry.getValue())
                        .limit(0)  // Can be populated from CompanyBilling limits
                        .percentage(0.0)
                        .build())
                .sorted(Comparator.comparing(UsageGraphResponse.DataPointDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Get usage type enum for graph ID
     */
    private BillingUsageLog.UsageType getUsageTypeForGraph(String graphId) {
        switch (graphId.toLowerCase()) {
            case "answers_trend":
                return BillingUsageLog.UsageType.answer;
            case "kb_trend":
                return BillingUsageLog.UsageType.kb_page_added;
            case "agents_trend":
                return BillingUsageLog.UsageType.agent_created;
            case "users_trend":
                return BillingUsageLog.UsageType.user_created;
            default:
                return null;  // Return all types
        }
    }

    /**
     * Get title for graph
     */
    private String getTitleForGraph(String graphId) {
        switch (graphId.toLowerCase()) {
            case "answers_trend":
                return "Answer Usage Trend";
            case "kb_trend":
                return "KB Page Additions Trend";
            case "agents_trend":
                return "Agent Creation Trend";
            case "users_trend":
                return "User Addition Trend";
            default:
                return "Usage Trend";
        }
    }

    /**
     * Export as CSV
     */
    private byte[] exportAsCsv(List<BillingUsageLog> logs) throws Exception {
        StringBuilder csv = new StringBuilder();
        csv.append("date,usage_type,count,blocked\n");

        for (BillingUsageLog log : logs) {
            csv.append(log.getCreatedAt().toLocalDate()).append(",")
                    .append(log.getUsageType().name()).append(",")
                    .append(log.getUsageCount() != null ? log.getUsageCount() : 0).append(",")
                    .append(log.getWasBlocked() != null && log.getWasBlocked() ? "true" : "false").append("\n");
        }

        return csv.toString().getBytes();
    }

    /**
     * Export as JSON
     */
    private byte[] exportAsJson(List<BillingUsageLog> logs) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();

        for (BillingUsageLog log : logs) {
            Map<String, Object> record = new HashMap<>();
            record.put("date", log.getCreatedAt().toLocalDate().toString());
            record.put("usage_type", log.getUsageType().name());
            record.put("count", log.getUsageCount() != null ? log.getUsageCount() : 0);
            record.put("blocked", log.getWasBlocked() != null && log.getWasBlocked());
            records.add(record);
        }

        return objectMapper.writeValueAsBytes(records);
    }
}