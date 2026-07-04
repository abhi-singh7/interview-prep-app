package com.interviewprep.output;

import java.util.List;

/**
 * Container for native structured output — OpenAI Structured Outputs does not support top-level JSON arrays.
 */
public record QuestionRecordList(List<QuestionRecord> questions) {
}
