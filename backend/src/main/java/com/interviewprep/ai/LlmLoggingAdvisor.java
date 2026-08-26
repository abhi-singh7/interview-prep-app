package com.interviewprep.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import com.interviewprep.domain.LlmInteractionLog;
import com.interviewprep.domain.LlmInteractionRepository;
import com.interviewprep.domain.LlmInteractionStatus;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Spring AI advisor that logs every LLM request and response passing through a {@link
 * org.springframework.ai.chat.client.ChatClient} — to the application log (SLF4J) AND as one row
 * per call in the {@code llm_interaction_log} table.
 * <p>
 * Console output: requests at INFO (one line per message, tagged with the client label so question
 * generation vs. evaluation traffic can be told apart); model options at DEBUG to keep default
 * output free of provider configuration details; responses log model name, token usage and full
 * content at INFO; errors logged at ERROR before being re-thrown.
 * <p>
 * Database persistence: one {@link LlmInteractionLog} row per call with the full request prompt
 * (all messages, role-tagged), response content, model, tokens, duration and SUCCESS/ERROR status.
 * Persistence failures are swallowed (warn) so a DB outage never breaks an LLM call.
 */
public class LlmLoggingAdvisor implements CallAdvisor, StreamAdvisor {

    /** Run outermost so the final prompt (after memory/tool advisors) is what gets logged. */
    private static final int ORDER = -100;

    private static final Logger log = LoggerFactory.getLogger(LlmLoggingAdvisor.class);

    private final String label;
    private final LlmInteractionRepository repository;

    public LlmLoggingAdvisor(String label, LlmInteractionRepository repository) {
        this.label = label;
        this.repository = repository;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startedAt = System.currentTimeMillis();
        logRequest(request);
        try {
            ChatClientResponse response = chain.nextCall(request);
            long durationMs = elapsed(startedAt);
            log.info("[LLM-{}] RESPONSE after {} ms", label, durationMs);
            logResponse(response);
            persistInteraction(request, response != null ? response.chatResponse() : null, durationMs, LlmInteractionStatus.SUCCESS, null);
            return response;
        } catch (RuntimeException ex) {
            long durationMs = elapsed(startedAt);
            log.error("[LLM-{}] REQUEST FAILED after {} ms: {}", label, durationMs, ex.toString());
            persistInteraction(request, null, durationMs, LlmInteractionStatus.ERROR, ex.toString());
            throw ex;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long startedAt = System.currentTimeMillis();
        logRequest(request);
        StringBuilder streamedContent = new StringBuilder();
        return chain.nextStream(request)
                .doOnNext(chunk -> {
                    // Each emitted chunk is one streamed generation delta.
                    logResponse(chunk);
                    String text = responseText(chunk != null ? chunk.chatResponse() : null);
                    if (text != null && !text.isEmpty()) {
                        streamedContent.append(text);
                    }
                })
                .doOnError(err -> {
                    long durationMs = elapsed(startedAt);
                    log.error("[LLM-{}] STREAM FAILED after {} ms: {}", label, durationMs, err.toString());
                    persistStream(request, streamedContent, durationMs, LlmInteractionStatus.ERROR, err.toString());
                })
                .doOnComplete(() -> {
                    long durationMs = elapsed(startedAt);
                    log.info("[LLM-{}] STREAM COMPLETED after {} ms", label, durationMs);
                    persistStream(request, streamedContent, durationMs, LlmInteractionStatus.SUCCESS, null);
                });
    }

    @Override
    public String getName() {
        return "llm-logging-advisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private void logRequest(ChatClientRequest request) {
        Prompt prompt = request.prompt();
        for (var message : prompt.getInstructions()) {
            log.info("[LLM-{}] REQUEST [{}]: {}", label, message.getMessageType(), message.getText());
        }
        // Options toString includes provider settings; keep it at DEBUG to avoid leaking config.
        if (prompt.getOptions() != null) {
            log.debug("[LLM-{}] OPTIONS: {}", label, prompt.getOptions());
        }
    }

    private void logResponse(ChatClientResponse clientResponse) {
        ChatResponse response = clientResponse.chatResponse();
        if (response == null || response.getResults().isEmpty()) {
            log.warn("[LLM-{}] EMPTY RESPONSE", label);
            return;
        }

        String content = responseText(response);

        ChatResponseMetadata metadata = response.getMetadata();
        String model = "unknown";
        Usage usage = null;
        if (metadata != null) {
            model = metadata.getModel() != null ? metadata.getModel() : "unknown";
            usage = metadata.getUsage();
        }

        log.info("[LLM-{}] RESPONSE content={}{}", label, content, usageSuffix(usage));
    }

    /** Write one interaction row; DB problems must never break the LLM call itself. */
    private void persistInteraction(ChatClientRequest request, ChatResponse response, long durationMs,
                                    LlmInteractionStatus status, String errorMessage) {
        try {
            LlmInteractionLog entry = new LlmInteractionLog();
            entry.setTimestamp(LocalDateTime.now());
            entry.setClientLabel(label);
            entry.setRequestPrompt(formatRequest(request));
            entry.setResponseContent(responseText(response));
            if (response != null && response.getMetadata() != null) {
                ChatResponseMetadata md = response.getMetadata();
                entry.setModel(md.getModel());
                Usage usage = md.getUsage();
                if (usage != null) {
                    entry.setPromptTokens(usage.getPromptTokens());
                    entry.setCompletionTokens(usage.getCompletionTokens());
                    entry.setTotalTokens(usage.getTotalTokens());
                }
            }
            entry.setDurationMs(durationMs);
            entry.setStatus(status);
            entry.setErrorMessage(errorMessage);
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("[LLM-{}] failed to persist interaction: {}", label, ex.toString(), ex);
        }
    }

    /** Streamed variant: one row per completed stream with the aggregated content. */
    private void persistStream(ChatClientRequest request, StringBuilder content, long durationMs,
                               LlmInteractionStatus status, String errorMessage) {
        try {
            LlmInteractionLog entry = new LlmInteractionLog();
            entry.setTimestamp(LocalDateTime.now());
            entry.setClientLabel(label);
            entry.setRequestPrompt(formatRequest(request));
            entry.setResponseContent(content.length() > 0 ? content.toString() : null);
            // Per-chunk token usage is not aggregated here; tokens stay null for streamed calls.
            entry.setDurationMs(durationMs);
            entry.setStatus(status);
            entry.setErrorMessage(errorMessage);
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("[LLM-{}] failed to persist stream interaction: {}", label, ex.toString(), ex);
        }
    }

    /** All request messages on one line each: [SYSTEM] ... \n [USER] ... */
    private String formatRequest(ChatClientRequest request) {
        Prompt prompt = request.prompt();
        return prompt.getInstructions().stream()
                .map(message -> "[" + message.getMessageType() + "] " + (message.getText() != null ? message.getText() : ""))
                .collect(Collectors.joining("\n"));
    }

    private String responseText(ChatResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            return null;
        }
        Generation generation = response.getResult();
        return generation != null && generation.getOutput() != null ? generation.getOutput().getText() : "";
    }

    private String usageSuffix(Usage usage) {
        if (usage == null) {
            return "";
        }
        // Prompt/completion tokens are reported by some providers; total is derived.
        return " model-tokens prompt=%s completion=%s total=%s".formatted(
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}
