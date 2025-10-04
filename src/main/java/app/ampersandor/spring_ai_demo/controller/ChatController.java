package app.ampersandor.spring_ai_demo.controller;


import app.ampersandor.spring_ai_demo.dto.EmotionEvaluation;
import app.ampersandor.spring_ai_demo.dto.PromptBody;
import app.ampersandor.spring_ai_demo.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
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
@RequestMapping("/chat")
class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
                          "userPrompt": "안녕하세요, 오늘 날씨 어때요?",
                          "systemPrompt": null,
                          "chatOptions": null
                        }
                        """
                            )
                    )
            )
    )
    @PostMapping(value = "/call", produces = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse call(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return this.chatService.call(promptBody.conversationId(), promptBuilder.build());
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
    Flux<String> stream(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return this.chatService.stream(promptBody.conversationId(), promptBuilder.build());
    }

    @Operation(summary = "감정 평가 응답", description = "입력된 prompt에 대해 감정 평가를 수행하고 결과를 반환합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PromptBody.class),
                            examples = @ExampleObject(
                                    name = "감정 평가 요청 예시",
                                    value = """
                    {
                      "conversationId": "conv-xyz",
                      "userPrompt": "이 제품 정말 좋네요!"
                    }
                    """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "감정 평가 결과 반환",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EmotionEvaluation.class),
                            examples = @ExampleObject(
                                    name = "응답 예시",
                                    value = """
                    {
                      "label": "POSITIVE",
                      "reason": [
                        "“정말 최고” → 매우 긍정적 표현.",
                        "“편해졌어요” → 실생활에 긍정적 영향."
                      ]
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping(value = "/emotion", produces = MediaType.APPLICATION_JSON_VALUE)
    EmotionEvaluation callEmotionEvaluation(@RequestBody @Valid PromptBody promptBody) {
        Prompt.Builder promptBuilder = getPromptBuilder(promptBody);
        return this.chatService.callEmotionEvaluation(promptBody.conversationId(), promptBuilder.build());
    }
}