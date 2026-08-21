package com.interviewprep.question;

import tools.jackson.databind.json.JsonMapper;

import com.interviewprep.output.EvaluationRecord;
import com.interviewprep.output.QuestionRecord;
import com.interviewprep.output.QuestionRecordList;
import com.interviewprep.domain.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiGatewayService {

    private final ChatClient questionChatClient;
    private final ChatClient evaluationChatClient;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final JsonMapper jsonMapper;

    public AiGatewayService(
            @org.springframework.beans.factory.annotation.Qualifier("questionChatClient") ChatClient questionChatClient,
            @org.springframework.beans.factory.annotation.Qualifier("evaluationChatClient") ChatClient evaluationChatClient,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            CategoryRepository categoryRepository,
            JsonMapper jsonMapper) {
        this.questionChatClient = questionChatClient;
        this.evaluationChatClient = evaluationChatClient;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.categoryRepository = categoryRepository;
        this.jsonMapper = jsonMapper;
    }

    public List<QuestionRecord> generateQuestions(String categoryId, String difficulty, int count, String languageName) {
        // For backward compatibility: if topicNames is null/empty, use categoryId as a single topic name
        return generateQuestions(categoryId, difficulty, count, languageName, List.of());
    }

    public List<QuestionRecord> generateQuestions(String categoryId, String difficulty, int count, String languageName, List<String> topics) {
        // Pass categoryId for context (shows "Category: X" when no topics provided), plus topic names if user selected them
        String prompt = buildGenerationPrompt(categoryId, difficulty, count, languageName, topics);
        
        var result = questionChatClient.prompt()
                .user(prompt)
                .call();

        return Objects.requireNonNull(result.entity(QuestionRecordList.class,
                spec -> spec.useProviderStructuredOutput().validateSchema())).questions();
    }

    public Evaluation evaluateTheoryAnswer(Question question, Answer answer) {
        String prompt = buildTheoryEvaluationPrompt(question, answer);
        
        var result = evaluationChatClient.prompt()
                .user(prompt)
                .call();

        var record = result.entity(EvaluationRecord.class, 
            spec -> spec.useProviderStructuredOutput().validateSchema());
        return mapToEvaluation(record, answer, question.getType());
    }

    public Evaluation evaluateCodingAnswer(Question question, Answer answer) {
        String languagePrompt = switch (answer.getLanguageSubmitted().toLowerCase()) {
            case "java" -> buildJavaCodingEvalSystemPrompt();
            case "python" -> buildPythonCodingEvalSystemPrompt();
            default -> buildJavascriptCodingEvalSystemPrompt();
        };

        String prompt = buildCodingEvaluationPrompt(question, answer);
        
        var result = evaluationChatClient.prompt()
                .system(s -> s.text(languagePrompt))
                .user(prompt)
                .call();

        var record = result.entity(EvaluationRecord.class, 
            spec -> spec.useProviderStructuredOutput().validateSchema());
        return mapToEvaluation(record, answer, question.getType());
    }

    private Evaluation mapToEvaluation(EvaluationRecord record, Answer answer, QuestionType type) {
        Evaluation eval = new Evaluation();
        eval.setAnswer(answer);
        eval.setStatus(EvaluationStatus.SUCCESS);
        eval.setScore(record.score());

        try {
            if (record.strengths() != null && !record.strengths().isEmpty()) {
                String jsonStr = jsonMapper.writeValueAsString(record.strengths());
                eval.setStrengthsJson(jsonStr);
            } else {
                eval.setStrengthsJson("[]");
            }
        } catch (tools.jackson.core.JacksonException e) {
            eval.setStrengthsJson("[]");
        }

        try {
            if (record.weaknesses() != null && !record.weaknesses().isEmpty()) {
                String jsonStr = jsonMapper.writeValueAsString(record.weaknesses());
                eval.setWeaknessesJson(jsonStr);
            } else {
                eval.setWeaknessesJson("[]");
            }
        } catch (tools.jackson.core.JacksonException e) {
            eval.setWeaknessesJson("[]");
        }

        if (record.improvedAnswer() != null) {
            eval.setImprovedAnswer(record.improvedAnswer());
        }

        if (type == QuestionType.CODE) {
            if (record.isCorrect() != null) {
                eval.setIsCorrect(record.isCorrect());
            }
            if (record.correctnessExplanation() != null) {
                eval.setCorrectnessExplanation(record.correctnessExplanation());
            }
            if (record.timeComplexity() != null) {
                eval.setTimeComplexity(record.timeComplexity());
            }
            if (record.spaceComplexity() != null) {
                eval.setSpaceComplexity(record.spaceComplexity());
            }
        }

        return eval;
    }

    private String buildGenerationPrompt(String categoryId, String difficulty, int count, String language) {
        return buildGenerationPrompt(categoryId, difficulty, count, language, List.of());
    }

    private String buildGenerationPrompt(String categoryId, String difficulty, int count, String language, java.util.List<String> topics) {
        // Build context display: show both Category and Topics if user selected specific topics
        List<String> parts = new java.util.ArrayList<>();
        if (categoryId != null && !categoryId.isBlank()) {
            parts.add("Category: " + categoryId);
        }
        if (topics != null && !topics.isEmpty()) {
            parts.add("Topics: " + String.join(", ", topics));
        }
        String topicDisplay = parts.isEmpty() ? "" : String.join("\n", parts);

        return """
                Generate %d interview questions for a technical interview preparation platform.
                
                %s
                
                Difficulty: %s
                Programming Language (for coding challenges): %s
                
                Requirements:
                - Include both THEORY and CODE type questions
                - For coding questions, provide title, description, and codePrompt (starter code)
                - Theory questions should focus on conceptual understanding of the topic(s)
                - Coding questions should test algorithmic thinking with practical scenarios
                - Difficulty level should be reflected in question complexity
                
                Return your response as a JSON array. Each object must have these exact fields:
                [
                  {
                    "type": "THEORY or CODE",
                    "topics": ["<topic name 1>", "<topic name 2>", ...],
                    "questionText": "<full question text>",
                    "title": "<only for CODE questions - short title>",
                    "description": "<only for CODE questions - detailed problem description>",
                    "codePrompt": "<only for CODE questions - starter code>"
                  }
                ]""".formatted(count, topicDisplay.isEmpty() ? "" : topicDisplay, difficulty, language);
    }

    private String buildTheoryEvaluationPrompt(Question question, Answer answer) {
        return """
                Evaluate this theory interview answer for a technical assessment.
                
                Question: %s
                Category: %s
                Difficulty: %s
                
                User's answer:
                %s
                
                Provide your evaluation as JSON with these exact fields:
                - score: integer 0-10 rating of the answer quality
                - strengths: array of strings listing what the user did well
                - weaknesses: array of strings listing areas for improvement
                - improved_answer: string with a better version of the answer
                
                Be fair and objective. Focus on technical accuracy, completeness, and clarity."""
            .formatted(
                question.getQuestionText(),
                question.getCategoryId() != null ? question.getCategoryId() : "N/A",
                question.getSession() != null && question.getSession().getDifficulty() != null ? question.getSession().getDifficulty() : "N/A",
                answer.getAnswerText()
            );
    }

    private String buildCodingEvaluationPrompt(Question question, Answer answer) {
        return """
                Evaluate this coding challenge submission for a technical assessment.
                
                Question Title: %s
                Description: %s
                
                User's submitted code (%s):
                ```%s
                %s
                ```
                
                Provide your evaluation as JSON with these exact fields:
                - is_correct: boolean indicating if the solution is algorithmically correct
                - correctness_explanation: string explaining correctness assessment
                - score: integer 0-10 rating of the solution quality
                - strengths: array of strings listing what was done well
                - weaknesses: array of strings listing areas for improvement
                - time_complexity: string describing the algorithmic time complexity (e.g., "O(n log n)")
                - space_complexity: string describing the algorithmic space complexity (e.g., "O(1)")
                - improved_solution: string with an optimized solution in the same language
                
                IMPORTANT: Evaluate algorithmic thinking, not syntax. Focus on correctness, efficiency, and edge case handling."""
            .formatted(
                question.getTitle() != null ? question.getTitle() : "N/A",
                question.getDescription() != null ? question.getDescription() : "N/A",
                answer.getLanguageSubmitted() != null ? answer.getLanguageSubmitted() : "unknown",
                answer.getLanguageSubmitted() != null ? answer.getLanguageSubmitted().toLowerCase() : "",
                answer.getAnswerText()
            );
    }

    private String buildJavaCodingEvalSystemPrompt() {
        return """
                You are a Java coding evaluator. Always evaluate using temperature=0 for deterministic results. When evaluating Java code submissions, focus on algorithmic correctness, time/space complexity, and best practices. Respond with valid JSON only.""";
    }

    private String buildPythonCodingEvalSystemPrompt() {
        return """
                You are a Python coding evaluator. Always evaluate using temperature=0 for deterministic results. When evaluating Python code submissions, focus on algorithmic correctness, time/space complexity, and idiomatic Python practices. Respond with valid JSON only.""";
    }

    private String buildJavascriptCodingEvalSystemPrompt() {
        return """
                You are a JavaScript/Angular coding evaluator. Always evaluate using temperature=0 for deterministic results. When evaluating JavaScript code submissions, focus on algorithmic correctness, time/space complexity, and modern JS best practices. Respond with valid JSON only.""";
    }
}
