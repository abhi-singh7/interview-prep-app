package com.interviewprep.domain;

import lombok.Data;
import java.util.List;

@Data
public class InterviewStartResponse {
    private Long sessionId;
    private List<Question> questions;
}
