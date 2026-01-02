package com.aiagent.controller;

import com.aiagent.dto.UsageStatisticsResponse;
import com.aiagent.dto.UsageStatisticsResponse.*;
import com.aiagent.entity.User;
import com.aiagent.repository.UserRepository;
import com.aiagent.service.UsageStatisticsService;
import com.aiagent.service.UsageStatisticsService.Period;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
@Tag(name = "Usage Statistics", description = "API usage statistics and monitoring")
public class UsageController {

    private final UsageStatisticsService usageStatisticsService;
    private final UserRepository userRepository;

    @GetMapping("/statistics")
    @Operation(summary = "Get usage statistics", description = "Get comprehensive usage statistics for the specified period")
    public ResponseEntity<UsageStatisticsResponse> getStatistics(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Time period for statistics")
            @RequestParam(defaultValue = "WEEK") String period
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Period periodEnum = Period.valueOf(period.toUpperCase());
        UsageStatisticsResponse stats = usageStatisticsService.getStatistics(user.getId(), periodEnum);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/overview")
    @Operation(summary = "Get quick overview stats", description = "Get a quick overview of usage statistics for the past week")
    public ResponseEntity<OverviewStats> getOverview(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        OverviewStats overview = usageStatisticsService.getQuickStats(user.getId());
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/agents/ranking")
    @Operation(summary = "Get agent usage ranking", description = "Get agents ranked by usage")
    public ResponseEntity<List<AgentStats>> getAgentRanking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Time period for statistics")
            @RequestParam(defaultValue = "WEEK") String period,
            @Parameter(description = "Number of top agents to return")
            @RequestParam(defaultValue = "10") int limit
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Period periodEnum = Period.valueOf(period.toUpperCase());
        List<AgentStats> ranking = usageStatisticsService.getAgentRanking(user.getId(), periodEnum, limit);

        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/export")
    @Operation(summary = "Export usage data", description = "Export usage statistics as CSV")
    public ResponseEntity<String> exportUsageData(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Time period for export")
            @RequestParam(defaultValue = "MONTH") String period
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Period periodEnum = Period.valueOf(period.toUpperCase());
        UsageStatisticsResponse stats = usageStatisticsService.getStatistics(user.getId(), periodEnum);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,API Calls,Conversations,Messages,Tokens\n");

        for (DailyStats day : stats.getDailyStats()) {
            csv.append(String.format("%s,%d,%d,%d,%d\n",
                    day.getDate(),
                    day.getApiCalls(),
                    day.getConversations(),
                    day.getMessages(),
                    day.getTokens()
            ));
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=usage-stats.csv")
                .body(csv.toString());
    }
}
