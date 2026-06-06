package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.metric.MetricType;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

// TODO: use virtual threads for slow blocking calls
public final class LLMClient extends Client{
    private final static Logger log = LogManager.getLogger(LLMClient.class.getName());

    private final static String BASE_URL  = "http://localhost:11434/v1";
    private final static String CHAT_COMPLETIONS = "/chat/completions";
    private final static String LLM_MODEL = "qwen2.5-coder:3b";
    private static final String SYSTEM_PROMPT =
            """
            You are a semantic code indexing engine.
            
            Your task is to generate concise technical summaries of source code for semantic search retrieval.
            
            Focus on:
            - the primary responsibility of the code
            - important business/domain concepts
            - important technical operations
            - frameworks, libraries, APIs, validations, database operations, caching, authentication, scheduling, messaging, or transformations if present
            
            Do NOT:
            - explain line-by-line implementation
            - mention variable names unless semantically important
            - generate markdown
            - generate bullet points
            - generate generic statements
            - mention obvious syntax details
            
            The summary must:
            - be concise
            - be information-dense
            - be optimized for semantic retrieval
            - describe what the code DOES
            
            Limit summary to 3-5 sentences.
            
            Output only the summary text.
            """;
    private static final String USER_PROMPT =
            """
            Generate a concise semantic summary for semantic code search indexing of a snippet of file %s.

            Focus on:
            - primary responsibility
            - important domain/business functionality
            - important technical operations
            
            Keep the summary concise, technical, and retrieval-friendly.
            
            Source code:
            %s
            """;

    private final ObjectMapper objectMapper;

    public LLMClient() {
        super();
        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public String generateSummary(String codeText, String fileName){
        log.debug("Calling Llama LLM for generating summary of {} characters.", codeText.length());

        LLMMessage systemMessage = new LLMMessage("system", SYSTEM_PROMPT);
        LLMMessage userMessage = new LLMMessage("user", USER_PROMPT.formatted(fileName, codeText));
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(List.of(systemMessage, userMessage));

        String requestUrl = BASE_URL + CHAT_COMPLETIONS;
        String requestBody = objectMapper.writeValueAsString(chatCompletionRequest);

        HttpResponse<String> response = executePost(requestUrl, requestBody, MetricType.LLM_GENERATE_SUMMARY);

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to generate LLM summary: " + response.body());
        }

        log.debug("Created LLM summary successfully for {} characters.", codeText.length());

        return extractSummary(response.body());
    }

    private String extractSummary(String response) {
        Map<String, Object> responseBody = (Map<String, Object>) objectMapper.readValue(response, Map.class);

        int tokens = (int) ((Map<String, Object>) responseBody.get("usage")).get("total_tokens");

        Map<String, Object> choice = ((List<Map<String, Object>>) responseBody.get("choices")).getFirst();
        String summary = ((String) ((Map<Object, Object>) choice.get("message")).get("content"));

        log.debug("{} tokens used. Generated summary: {}", tokens, summary);
        return summary;
    }

    private record ChatCompletionRequest(String model, List<LLMMessage> messages){
        public ChatCompletionRequest(List<LLMMessage> messages){
            this(LLM_MODEL, messages);
        }
    }

    private record LLMMessage(String role, String content){}
}
