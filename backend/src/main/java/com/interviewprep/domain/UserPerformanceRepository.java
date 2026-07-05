package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link UserPerformance} entities.
 * <p>
 * This repository provides CRUD operations for user performance records, which store aggregated
 * analytics data (total sessions, average scores, category breakdowns) computed from raw interview session data.
 * The service layer handles the complex aggregation logic; this repository simply persists and retrieves
 * the pre-computed results.
 * </p>
 * 
 * @see UserPerformance
 */
@Repository
public interface UserPerformanceRepository extends JpaRepository<UserPerformance, Long> {
}
