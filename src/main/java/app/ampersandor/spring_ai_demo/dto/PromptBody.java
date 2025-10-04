package app.ampersandor.spring_ai_demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.ai.chat.prompt.DefaultChatOptions;

public record PromptBody(
            @NotEmpty @Schema(description = "대화 식별자", example = "conv-1234") String conversationId,
            @NotEmpty @Schema(description = "사용자 입력 프롬프트", example = "안녕하세요, 오늘 날씨 어때요?") String userPrompt,
            @Nullable @Schema(description = "시스템 프롬프트(선택)", example = "You are a helpful assistant.") String systemPrompt,
            // serialize 를 위해서 interface 인 ChatOptions 이 아니라 DefaultChatpOptions을 받는다.
            @Nullable @Schema(description = "채팅 옵션(선택)", implementation = DefaultChatOptions.class) DefaultChatOptions chatOptions
    ) {}