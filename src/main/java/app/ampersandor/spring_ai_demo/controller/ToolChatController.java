package app.ampersandor.spring_ai_demo.controller;

import app.ampersandor.spring_ai_demo.dto.PromptBody;
import app.ampersandor.spring_ai_demo.service.ToolChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@RequestMapping("/tool")
@ConditionalOnProperty(name = "app.mode", havingValue = "tool")
class ToolChatController {

    private final ToolChatService toolChatService;

    public ToolChatController(ToolChatService toolChatService) {
        this.toolChatService = toolChatService;
    }

    @Operation(
            summary = "단일 응답 채팅",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PromptBody.class),
                            examples = @ExampleObject(
                                    name = "예시",
                                    value = """
                        {
                          "conversationId": "conv-1234",
                          "userPrompt": "안녕하세요, 제주도 날씨 어때요?",
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
    ChatResponse call(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return this.toolChatService.call(promptBody.conversationId(), promptBuilder.build());
    }


    @Operation(
            summary = "채팅 스트리밍 응답 (SSE)",
            description = "PromptBody를 받아 이벤트 스트림으로 응답을 반환합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PromptBody.class),
                            examples = @ExampleObject(
                                    name = "스트림 예시",
                                    summary = "Streaming chat 예시",
                                    value = """
                    {
                      "conversationId": "conv-5678",
                      "userPrompt": "서울 날씨 자세히",
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
    Flux<String> stream(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return this.toolChatService.stream(promptBody.conversationId(), promptBuilder.build());
    }

    private static Prompt.Builder getPromptBuilder(PromptBody promptBody) {
        List<Message> messages = new ArrayList<>();
        Optional.ofNullable(promptBody.systemPrompt()).filter(Predicate.not(String::isBlank))
                .map(systemPrompt -> SystemMessage.builder().text(systemPrompt).build()).ifPresent(messages::add);
        messages.add(UserMessage.builder().text(promptBody.userPrompt()).build());
        Prompt.Builder promptBuilder = Prompt.builder().messages(messages);
        Optional.ofNullable(promptBody.chatOptions()).ifPresent(promptBuilder::chatOptions);
        return promptBuilder;
    }

}
