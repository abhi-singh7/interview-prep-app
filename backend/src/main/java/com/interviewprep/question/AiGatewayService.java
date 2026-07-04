package com.interviewprep.question;

import tools.jackson.databind.json.JsonMapper;

import com.interviewprep.output.EvaluationRecord;
import com.interviewprep.output.QuestionRecord;
import com.interviewprep.output.QuestionRecordList;
import com.interviewprep.domain.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service handling AI-powered question generation and answer evaluation via Spring AI ChatClient.
 * 
 * <p>This service acts as the bridge between the application domain and the AI backend (LM Studio or OpenAI-compatible).
 * It uses two separate {@link ChatClient} beans: one for generating interview questions ({@code questionChatClient})
 * and another for evaluating user answers ({@code evaluationChatClient}).</p>
 * 
 * <h3>Question Generation Flow:</h3>
 * <ol>
 *   <li>Construct a structured prompt with category context, topic list, difficulty level, and language</li>
 *   <li>Invoke the question chat client with native structured output (Spring AI)</li>
 *   <li>Parse the JSON response into {@link QuestionRecord} objects</li>
 * </ol>
 * 
 * <h3>Evaluation Flow:</h3>
 * <p>For each user answer, build a language-specific evaluation prompt and invoke the evaluation chat client.
 * Coding answers receive per-language system prompts (Java, Python, JavaScript) for domain-appropriate feedback.</p>
 */
@Service
public class AiGatewayService {

    private final ChatClient questionChatClient;
    private final ChatClient evaluationChatClient;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final JsonMapper jsonMapper;

    public AiGatewayService(
            @Qualifier("questionChatClient") ChatClient questionChatClient,
            @Qualifier("evaluationChatClient") ChatClient evaluationChatClient,
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

    /**
     * Generates interview questions via AI for the given category and difficulty.
     * Delegates to the 5-arg overload with an empty topics list, using categoryId as a single topic context.
     * 
     * @param categoryId the category (language) ID for question generation context
     * @param difficulty the difficulty level string (e.g., "easy", "medium", "hard")
     * @param count the number of questions to generate (typically 1-50)
     * @param languageName the programming language name for coding challenge context
     * @return list of generated question records with type, text, and optional code scaffolding
     */
    public List<QuestionRecord> generateQuestions(String categoryId, String difficulty, int count, String languageName) {
        // For backward compatibility: if topicNames is null/empty, use categoryId as a single topic name
        return generateQuestions(categoryId, difficulty, count, languageName, List.of());
    }

    /**
     * Generates interview questions via AI with full topic context for the given category and difficulty.
     * 
     * <p>Constructs a structured prompt including category display, topic list, difficulty level,
     * and target programming language. The AI returns either THEORY or CODE type questions using
     * Spring AI native structured output to enforce JSON schema compliance.</p>
     * 
     * @param categoryId the category (language) ID for question generation context
     * @param difficulty the difficulty level string (e.g., "easy", "medium", "hard")
     * @param count the number of questions to generate (typically 1-50)
     * @param languageName the programming language name for coding challenge context
     * @param topics list of topic names to focus question generation on; empty list means broad category coverage
     * @return list of generated question records with type, text, and optional code scaffolding
     */
    public List<QuestionRecord> generateQuestions(String categoryId, String difficulty, int count, String languageName, List<String> topics) {
        // Pass categoryId for context (shows "Category: X" when no topics provided), plus topic names if user selected them
        String prompt = buildGenerationPrompt(categoryId, difficulty, count, languageName, topics);
        
        var result = questionChatClient.prompt()
                .user(prompt)
                .call();

        return Objects.requireNonNull(result.entity(QuestionRecordList.class,
                spec -> spec.useProviderStructuredOutput().validateSchema())).questions();
    }

    /**
     * Evaluates a theory (non-coding) answer submitted by the user.
     * 
     * <p>Builds an evaluation prompt with the question text, category, difficulty level,
     * and the user's answer. The AI returns a structured response with a numeric score (0-10),
     * strengths/weaknesses arrays, and an improved answer suggestion.</p>
     * 
     * @param question the original theory question being evaluated
     * @param answer the user's submitted answer text
     * @return an {@link Evaluation} entity with score, strengths, weaknesses, and improvement suggestions
     */
    public Evaluation evaluateTheoryAnswer(Question question, Answer answer) {
        String prompt = buildTheoryEvaluationPrompt(question, answer);
        
        var result = evaluationChatClient.prompt()
                .user(prompt)
                .call();

        var record = result.entity(EvaluationRecord.class, 
            spec -> spec.useProviderStructuredOutput().validateSchema());
        return mapToEvaluation(record, answer, question.getType());
    }

    /**
     * Evaluates a coding challenge submission with language-specific evaluation criteria.
     * 
     * <p>Selects a per-language system prompt (Java, Python, or JavaScript) based on the submitted
     * code's language, then builds a coding-focused evaluation prompt requesting algorithmic correctness
     * assessment, time/space complexity analysis, and an improved solution.</p>
     * 
     * @param question the original coding question being evaluated
     * @param answer the user's submitted code answer
     * @return an {@link Evaluation} entity with correctness flag, score, complexity analysis, and improvement suggestion
     */
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


    /**
     * Constructs the AI prompt template for question generation.
     * 
     * <p>The prompt includes category context display (e.g., "Category: Java"), optional topic list,
     * target difficulty level, and programming language. It instructs the LLM to produce both THEORY
     * and CODE questions with strict JSON schema compliance using native structured output.</p>
     * 
     * <p>If no topics are provided but a categoryId exists, only "Category: X" is displayed in context.
     * When topics are specified, both category and topic names appear for focused generation.</p>
     */
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
                - Include both THEORY and CODE type questions, 50 percent each
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

    /**
     * Builds the evaluation prompt for theory (non-coding) answers.
     * 
     * <p>The prompt includes the original question text, category, difficulty level, and user's answer.
     * It instructs the AI to return a structured JSON response with score (0-10), strengths, weaknesses,
     * and an improved version of the answer.</p>
     */
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

    /**
     * Builds the evaluation prompt for coding challenge submissions.
     * 
     * <p>The prompt includes the question title, description, the user's submitted code in fenced format,
     * and instructions to evaluate algorithmic correctness (not syntax). It requests is_correct boolean,
     * time/space complexity analysis, strengths/weaknesses, and an improved solution.</p>
     */
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
