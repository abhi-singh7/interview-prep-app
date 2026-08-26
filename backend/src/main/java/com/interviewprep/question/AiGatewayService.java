package com.interviewprep.question;

import tools.jackson.databind.json.JsonMapper;

import com.interviewprep.output.EvaluationRecord;
import com.interviewprep.output.QuestionRecord;
import com.interviewprep.output.QuestionRecordList;
import com.interviewprep.domain.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiGatewayService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiGatewayService.class);

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

        QuestionRecordList list = Objects.requireNonNull(result.entity(QuestionRecordList.class,
                spec -> spec.useProviderStructuredOutput().validateSchema()));
        return normalizeGeneratedRecords(list.questions());
    }

    // The local model sometimes swaps the type label while balancing the THEORY/CODE mix.
    // Reconcile each record's type with its actual content so FE editor, evaluator selection
    // and reference grading all agree on what kind of question this is.
    private List<QuestionRecord> normalizeGeneratedRecords(List<QuestionRecord> records) {
        return records.stream().map(this::reconcileRecord).toList();
    }

    private QuestionRecord reconcileRecord(QuestionRecord r) {
        // Strip the model's cosmetic "CODE:" / "THEORY:" prefix from references so they render cleanly.
        String reference = REF_TYPE_PREFIX.matcher(nullSafe(r.referenceAnswer())).replaceFirst("").stripLeading();

        String declared = nullSafe(r.type()).strip().toUpperCase(Locale.ROOT);
        boolean hasStarterCode = !nullSafe(r.codePrompt()).isBlank();
        String questionText = nullSafe(r.questionText()).strip();
        String description = nullSafe(r.description()).strip();

        String fixedType;
        if (hasStarterCode && !declared.equals("CODE")) {
            // Starter code only exists for coding tasks — label was swapped.
            fixedType = "CODE";
        } else if (declared.equals("CODE") && ESSAY_START.matcher(questionText).find()
                && (description.isEmpty() || ESSAY_START.matcher(description).find())) {
            // Essay prompt with no code fields — this is a theory question mislabeled CODE.
            fixedType = "THEORY";
        } else {
            return new QuestionRecord(r.type(), r.topics(), r.questionText(), r.title(), r.description(), r.codePrompt(), reference);
        }

        log.warn("Reconciled generated question type '{}' -> '{}' (content-based): {}",
                declared, fixedType, questionText.substring(0, Math.min(60, questionText.length())));
        return new QuestionRecord(fixedType, r.topics(), r.questionText(), r.title(), r.description(), r.codePrompt(), reference);
    }

    private static final java.util.regex.Pattern ESSAY_START =
            java.util.regex.Pattern.compile("(?i)^(explain|describe|define|discuss|compare|differentiate|elaborate|what is|how does)");
    private static final java.util.regex.Pattern REF_TYPE_PREFIX =
            java.util.regex.Pattern.compile("^\\s*(CODE|THEORY)\\s*:\\s*");

    private static String nullSafe(String s) { return s != null ? s : ""; }

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
        // FIX-14: null-guard languageSubmitted (code answers may omit it); default to JavaScript
        String submittedLanguage = answer.getLanguageSubmitted() != null ? answer.getLanguageSubmitted().toLowerCase() : "javascript";
        String languagePrompt = switch (submittedLanguage) {
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

    public String generateReferenceAnswer(Question question) {
        String prompt = buildReferenceAnswerPrompt(question);

        var result = evaluationChatClient.prompt()
                .user(prompt)
                .call();

        return result.content();
    }

    private String buildReferenceAnswerPrompt(Question question) {
        String questionBlock;
        if (question.getType() == QuestionType.CODE) {
            questionBlock = "Question Title: %s%nDescription:%n%s"
                .formatted(
                    question.getTitle() != null ? question.getTitle() : "N/A",
                    question.getDescription() != null ? question.getDescription() : "N/A");
        } else {
            questionBlock = "Question: %s".formatted(question.getQuestionText());
        }

        if (question.getType() == QuestionType.CODE) {
            return """
                    Provide the ideal reference answer for this CODING interview question.

                    %s

                    Write a COMPLETE, RUNNABLE solution as actual code inside a fenced code block - real implementation code that solves the problem end-to-end, never prose, pseudocode, or step-by-step instructions. Match the language of the starter code when one is shown; otherwise use Java. After the code block you may add at most two lines stating the approach and time/space complexity.
                    Respond in plain text only - do not wrap it in JSON."""
                .formatted(questionBlock);
        }

        return """
                Provide the ideal reference answer for this THEORY interview question.

                %s

                Write a complete, technically correct model answer that demonstrates what a strong candidate should provide: cover all key concepts with clear explanations and trade-offs.
                Respond in plain text only - do not wrap it in JSON."""
            .formatted(questionBlock);
    }

    private Evaluation mapToEvaluation(EvaluationRecord record, Answer answer, QuestionType type) {
        Evaluation eval = new Evaluation();
        eval.setAnswer(answer);
        eval.setStatus(EvaluationStatus.SUCCESS);
        // FIX-22: the model can return out-of-range scores (e.g. 42 or -1); clamp to [0, 10]
        // so the `/10` display and analytics averages stay valid.
        int rawScore = record.score();
        int score = Math.max(0, Math.min(10, rawScore));
        if (score != rawScore) {
            log.warn("Clamped out-of-range evaluation score: {} -> {}", rawScore, score);
        }
        eval.setScore(score);

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
                 - Include both THEORY and CODE type questions. Decide each question's type by its ideal answer format BEFORE writing: if the ideal answer is working code it is CODE, if it is a prose explanation it is THEORY — the label must always match the content
                 - For coding questions, provide title, description, and codePrompt (starter code)
                 - Theory questions should focus on conceptual understanding of the topic(s)
                 - Coding questions should test algorithmic thinking with practical scenarios
                 - Difficulty level should be reflected in question complexity
                 - Every question MUST include a referenceAnswer following its type rules:
                     CODE questions: a COMPLETE, RUNNABLE solution written as actual code in the programming language listed above (use Java when no language is specified) - the real implementation, never prose, pseudocode, or step-by-step instructions. You may add at most one line after the code stating time/space complexity.
                     THEORY questions: a concise ideal answer covering all key concepts with clear explanations and trade-offs.
                 
                 Return your response as a JSON array. Each object must have these exact fields:
                 [
                   {
                     "type": "THEORY or CODE",
                     "topics": ["<topic name 1>", "<topic name 2>", ...],
                     "questionText": "<full question text>",
                     "title": "<only for CODE questions - short title>",
                     "description": "<only for CODE questions - detailed problem description>",
                     "codePrompt": "<only for CODE questions - starter code>",
                     "referenceAnswer": "<CODE: complete runnable code; THEORY: concise ideal answer covering all key points>"
                   }
                 ]""".formatted(count, topicDisplay.isEmpty() ? "" : topicDisplay, difficulty, language);
    }

    private String buildTheoryEvaluationPrompt(Question question, Answer answer) {
        return """
                Evaluate this theory interview answer for a technical assessment.
                
                Question: %s
                Category: %s
                Difficulty: %s
                %s
                User's answer:
                %s
                
                Provide your evaluation as JSON with these exact fields:
                - score: integer 0-10 rating of the answer quality
                - strengths: array of strings listing what the user did well
                - weaknesses: array of strings listing areas for improvement
                - improved_answer: string with a better version of the answer
                
                Scoring rubric (grade against the reference answer):
                - 9-10: covers all key points of the reference answer, technically accurate and clear
                - 5-7: mostly correct with minor gaps, imprecise terminology, or missing secondary details
                - 3-4: partially correct; several key points wrong or missing
                - 0-2: largely incorrect, off-topic, or nearly empty
                
                Be fair and objective. Focus on technical accuracy, completeness, and clarity."""
            .formatted(
                question.getQuestionText(),
                question.getCategoryId() != null ? question.getCategoryId() : "N/A",
                question.getSession() != null && question.getSession().getDifficulty() != null ? question.getSession().getDifficulty() : "N/A",
                referenceAnswerBlock(question),
                answer.getAnswerText()
            );
    }

    /** Grading standard for evaluations: the stored ideal answer, when present (legacy questions lack one). */
    private String referenceAnswerBlock(Question question) {
        if (question.getReferenceAnswer() == null || question.getReferenceAnswer().isBlank()) {
            return "Reference Answer: not available - judge using established technical knowledge.";
        }
        return """
                Reference Answer (grading standard):
                %s""".formatted(question.getReferenceAnswer());
    }

    private String buildCodingEvaluationPrompt(Question question, Answer answer) {
        return """
                Evaluate this coding challenge submission for a technical assessment.
                
                Question Title: %s
                Description: %s
                Difficulty: %s
                %s
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
                
                Scoring rubric (grade against the reference answer):
                - 9-10: correct, efficient solution matching the reference approach and edge cases
                - 5-7: works for main cases but suboptimal complexity or missing some edge cases
                - 3-4: partially working; major bugs, poor efficiency, or most edge cases unhandled
                - 0-2: does not run / largely incorrect or nearly empty
                
                IMPORTANT: Evaluate algorithmic thinking, not syntax. Focus on correctness, efficiency, and edge case handling."""
            .formatted(
                question.getTitle() != null ? question.getTitle() : "N/A",
                question.getDescription() != null ? question.getDescription() : "N/A",
                question.getSession() != null && question.getSession().getDifficulty() != null ? question.getSession().getDifficulty() : "N/A",
                referenceAnswerBlock(question),
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
