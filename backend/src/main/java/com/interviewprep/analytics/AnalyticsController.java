package com.interviewprep.analytics;
import com.interviewprep.config.AuthenticationContext;
import com.interviewprep.domain.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller responsible for exposing analytics-related endpoints.
 *
 * This controller serves as the entry point for retrieving user performance metrics,
 * including aggregated scores, category-wise breakdowns, and session summaries.
 * It leverages {@link AnalyticsService} for business logic and {@link AuthenticationContext}
 * to identify the currently authenticated user.
 *
 * @see AnalyticsService
 * @see AuthenticationContext
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthenticationContext authContext;

    /**
     * Constructs the AnalyticsController with necessary dependencies.
     *
     * @param analyticsService The service layer handling complex analytics calculations.
     * @param authContext The security context used to extract the current user's identity.
     */
    public AnalyticsController(AnalyticsService analyticsService, AuthenticationContext authContext) {
        this.analyticsService = analyticsService;
        this.authContext = authContext;
    }

    /**
     * Retrieves a comprehensive performance report for the currently authenticated user.
     *
     * This endpoint fetches aggregated data such as:
     * <ul>
     *     <li>Average scores across all sessions.</li>
     *     <li>Breakdown of performance by category.</li>
     *     <li>Summaries of individual interview sessions.</li>
     * </ul>
     *
     * @return A {@link ResponseEntity} containing the {@link AnalyticsResponse} object
     *         with the user's performance metrics, or a 403/401 error if the user is not authenticated.
     */
    @GetMapping("/performance")
    public ResponseEntity<AnalyticsResponse> getPerformanceData() {
        Long userId = authContext.getCurrentUserId();
        AnalyticsResponse data = analyticsService.getPerformanceData(userId);
        return ResponseEntity.ok(data);
    }
}
