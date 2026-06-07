package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

// TODO: use virtual threads for slow blocking calls
public final class LLMClient extends Client{
    private final static Logger log = LogManager.getLogger(LLMClient.class.getName());

    private static final String SERVICE_NAME = "LLM";

    private final static int RETRY_COUNTER = 3;
    private final static String BASE_URL  = "http://localhost:11434/v1";
    private final static String CHAT_COMPLETIONS = "/chat/completions";
    private final static String LLM_MODEL = "qwen2.5-coder:3b";
    private static final String SYSTEM_PROMPT =
            """
            You are a semantic code indexing engine.
    
            Generate concise technical summaries of source files for semantic search retrieval.
    
            For each file:
             - describe the primary responsibility
             - describe important business/domain functionality
             - describe important technical operations such as database access, validation, authentication, caching, scheduling, messaging, API integration, data transformation, or background processing
    
            Do not:
             - explain implementation details
             - describe code structure
             - mention variables unless semantically important
             - generate markdown
             - generate bullet points
    
            Summaries must be concise, information-dense, and optimized for semantic retrieval.
            """;
    private static final String USER_PROMPT =
            """
            Generate a semantic search summary for the following %d source file(s): %s

            Requirements:
             - summarize files independently
             - do not mix information between files
             - focus on responsibilities and behavior
             - limit each summary to 4-5 sentences
            
            Your response will be directly parsed as JSON list. Don't mention unnecessary text and return ONLY valid JSON with schema:
             [
               {
                 "file": "FILE_NAME",
                 "summary": "SUMMARY"
               }
             ]
            
            Files:
            
            %s
            """;

    private final ObjectMapper objectMapper;

    public LLMClient() {
        super(SERVICE_NAME);
        objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .propertyNamingStrategy(new PropertyNamingStrategies.NamingBase() {
                    @Override
                    public String translate(String propertyName) {
                        return propertyName.trim();
                    }

                    @Override
                    public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam, String defaultName) {
                        return defaultName.trim();
                    }
                })
                .build();
    }

    public List<String> generateSummary(List<String> texts, List<Path> paths) {

        Map.Entry<String, String> fileSection = createFileSection(texts, paths);
        String fileNames =  fileSection.getKey();
        String fileContents = fileSection.getValue();

        log.debug("Calling LLM for generating summaries of {} files ({} chars).", texts.size(), fileContents.length());

        LLMMessage systemMessage = new LLMMessage("system", SYSTEM_PROMPT);
        LLMMessage userMessage = new LLMMessage("user", USER_PROMPT.formatted(texts.size(), fileNames, fileContents));
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(List.of(systemMessage, userMessage));

        String requestUrl = BASE_URL + CHAT_COMPLETIONS;
        String requestBody = objectMapper.writeValueAsString(chatCompletionRequest);
        List<String> result = null;

        log.trace("LLM request body: {}", requestBody);

        for(int i=0; i<RETRY_COUNTER; i++) {
            HttpResponse<String> response = executePost(requestUrl, requestBody, MetricType.LLM_GENERATE_SUMMARY);

            if (response.statusCode() != 200) {
                log.error("LLM chat-completions failed: status={} body={}",
                        response.statusCode(), response.body());
                throw new SemseaException("LLM service '" + LLM_MODEL + "' returned an unexpected response.");
            }

            log.debug("Created LLM summary successfully for {} characters.", fileContents.length());

            try {
                result = extractSummary(response.body(), texts.size());
                break;
            }
            catch (IllegalStateException | JacksonException e) {
                log.warn("LLM returned malformed summary (attempt {} of {})", i + 1, RETRY_COUNTER, e);
                result = null;
            }
        }

        if(result == null) {
            throw new SemseaException("LLM service '" + LLM_MODEL + "' could not produce a usable summary after retries.");
        }

        return result;
    }

    private Map.Entry<String, String> createFileSection(List<String> texts, List<Path> paths) {
        StringBuilder fileContents = new StringBuilder();
        StringBuilder fileNames = new StringBuilder();

        for(int i = 0; i < texts.size(); i++) {

            String fileName = paths.get(i).getFileName().toString();

            fileNames.append(fileName);
            if(i != texts.size() - 1) {
                fileNames.append(", ");
            }

            fileContents.append("FILE_")
                    .append(i)
                    .append(": ")
                    .append(fileName)
                    .append('\n');

            fileContents.append(texts.get(i));

            fileContents.append("\n\n");
        }

        return Map.entry(fileNames.toString(), fileContents.toString());
    }

    private List<String> extractSummary(String response, int expectedSize) {

        Map<String, Object> responseBody = (Map<String, Object>) objectMapper.readValue(response, Map.class);

        int tokens = (int) ((Map<String, Object>) responseBody.get("usage")).get("total_tokens");

        Map<String, Object> choice = ((List<Map<String, Object>>) responseBody.get("choices")).getFirst();
        String content = ((String) ((Map<Object, Object>) choice.get("message")).get("content")).trim();

        log.debug("LLM consumed {} tokens; response length={} chars", tokens, content.length());
        log.trace("LLM raw response content: {}", content);

        if(content.startsWith("```json")) {
            content = content.substring(7, content.length() - 3).trim();
        }
        else if(content.startsWith("```")) {
            content = content.substring(3, content.length() - 3).trim();
        }


        List<FileSummaryResponse> summaries = objectMapper.readValue(content, new TypeReference<>() {});
        if(summaries.size() != expectedSize) {
            throw new IllegalStateException("LLM returned malformed JSON: " + content);
        }

        return summaries.stream()
                .map(FileSummaryResponse::summary)
                .toList();
    }

    private record ChatCompletionRequest(String model, List<LLMMessage> messages){
        public ChatCompletionRequest(List<LLMMessage> messages){
            this(LLM_MODEL, messages);
        }
    }

    private record LLMMessage(String role, String content){}

    public record FileSummaryResponse(String file, String summary) {}
}
