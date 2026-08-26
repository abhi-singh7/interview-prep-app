package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LlmInteractionRepository extends JpaRepository<LlmInteractionLog, Long> {

    List<LlmInteractionLog> findByClientLabelOrderByTimestampDesc(String clientLabel);

    List<LlmInteractionLog> findByTimestampAfterOrderByTimestampAsc(LocalDateTime after);
}
