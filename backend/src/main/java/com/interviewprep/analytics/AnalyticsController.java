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

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthenticationContext authContext;

    public AnalyticsController(AnalyticsService analyticsService, AuthenticationContext authContext) {
        this.analyticsService = analyticsService;
        this.authContext = authContext;
    }

    @GetMapping("/performance")
    public ResponseEntity<AnalyticsResponse> getPerformanceData() {
        Long userId = authContext.getCurrentUserId();
        AnalyticsResponse data = analyticsService.getPerformanceData(userId);
        return ResponseEntity.ok(data);
    }
}
