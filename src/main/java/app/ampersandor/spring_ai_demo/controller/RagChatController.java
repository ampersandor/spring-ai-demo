package app.ampersandor.spring_ai_demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import app.ampersandor.spring_ai_demo.service.RagChatService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@RestController
@RequestMapping("/rag")
class RagChatController {

    private final RagChatService ragChatService;

    public RagChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    /**
     * Request payload for Retrieval-Augmented chat endpoints.
     * {@link DefaultChatOptions} is used instead of the interface so Spring can materialize it from JSON.
     */
    public record RagPromptBody(
            @NotEmpty @Schema(description = "대화 식별자", example = "conv-1234") String conversationId,
            @NotEmpty @Schema(description = "사용자 입력 프롬프트", example = "안녕하세요, 오늘 날씨 어때요?") String userPrompt,
            @Nullable @Schema(description = "시스템 프롬프트(선택)", example = "You are a helpful assistant.") String systemPrompt,
            @Nullable @Schema(description = "채팅 옵션(선택)", implementation = DefaultChatOptions.class) DefaultChatOptions chatOptions,
            @Nullable @Schema(description = "VectorStore FilterExpression", example = "source == 'document1.pdf'") String filterExpression
    ) {}

    /**
     * Standard REST call that wraps the prompt, executes RAG and returns the fully materialized {@link ChatResponse}.
     * A filter expression can be passed to narrow the candidate documents stored in the vector store.
     */
    @Operation(
            summary = "단일 응답 채팅",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RagPromptBody.class),
                            examples = @ExampleObject(
                                    name = "예시",
                                    value = """
                        {
                          "conversationId": "conv-1234",
                          "userPrompt": "안녕하세요, 오늘 날씨 어때요?",
                          "systemPrompt": null,
                          "chatOptions": null,
                          "filterExpression": null
                        }
                        """
                            )
                    )
            )
    )
    @PostMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse call(@RequestBody @Valid RagPromptBody ragPromptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(ragPromptBody);
        return this.ragChatService.call(ragPromptBody.conversationId, promptBuilder.build(),
                Optional.ofNullable(ragPromptBody.filterExpression()));
    }

    /**
     * Utility used by both call and stream endpoints.
     * The logic mirrors {@link app.ampersandor.spring_ai_demo.controller.ChatController#getPromptBuilder} but with RAG-specific types.
     */
    private static Prompt.Builder getPromptBuilder(RagPromptBody ragPromptBody) {
        List<Message> messages = new ArrayList<>();
        Optional.ofNullable(ragPromptBody.systemPrompt).filter(Predicate.not(String::isBlank))
                .map(systemPrompt -> SystemMessage.builder().text(systemPrompt).build()).ifPresent(messages::add);
        messages.add(UserMessage.builder().text(ragPromptBody.userPrompt).build());
        Prompt.Builder promptBuilder = Prompt.builder().messages(messages);
        Optional.ofNullable(ragPromptBody.chatOptions).ifPresent(promptBuilder::chatOptions);
        return promptBuilder;
    }

    /**
     * Streams RAG answers while still honoring the optional filter expression.
     * Downstream clients receive tokens as soon as the model produces them.
     */
    @Operation(
            summary = "채팅 스트리밍 응답 (SSE)",
            description = "PromptBody를 받아 이벤트 스트림으로 응답을 반환합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RagPromptBody.class),
                            examples = @ExampleObject(
                                    name = "스트림 예시",
                                    summary = "Streaming chat 예시",
                                    value = """
                    {
                      "conversationId": "conv-5678",
                      "userPrompt": "안녕하세요, 뉴스 알려줘",
                      "systemPrompt": null,
                      "chatOptions": {
                        "maxTokens": 100,
                        "temperature": 0.7
                      }
                    }
                    """
                            )
                    )
            )
    )
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestBody @Valid RagPromptBody ragPromptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(ragPromptBody);
        return this.ragChatService.stream(ragPromptBody.conversationId, promptBuilder.build(),
                Optional.ofNullable(ragPromptBody.filterExpression()));
    }

}
