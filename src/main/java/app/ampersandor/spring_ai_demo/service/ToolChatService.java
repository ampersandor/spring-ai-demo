package app.ampersandor.spring_ai_demo.service;

import app.ampersandor.spring_ai_demo.tool.Tools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "tool")
public class ToolChatService {

    private final ChatClient chatClient;

    public ToolChatService(ChatClient.Builder chatClientBuilder, Advisor[] advisors,
                           @Value("${app.chat.default-system-prompt:}") String systemPrompt, Tools tools) {
        // Tool 에서 제공한 내용을 기반으로 정보를 생성 해야 하므로 temperature를 0.2 로 설정
        this.chatClient = chatClientBuilder.defaultSystem(systemPrompt)
                .defaultTools(tools)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(true) // 생략해도 true가 기본값
                        .temperature(0.2)
                        .build()).defaultAdvisors(advisors).build();
    }

    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(String conversationId, Prompt prompt) {
        return chatClient.prompt(prompt)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId));
    }

    public Flux<String> stream(String conversationId, Prompt prompt) {
        return buildChatClientRequestSpec(conversationId, prompt).stream().content().checkpoint();
    }

    public ChatResponse call(String conversationId, Prompt prompt) {
        return buildChatClientRequestSpec(conversationId, prompt).call().chatResponse();
    }

}
