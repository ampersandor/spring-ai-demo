package app.ampersandor.spring_ai_demo.service;


import app.ampersandor.spring_ai_demo.domain.Emotion;
import app.ampersandor.spring_ai_demo.dto.EmotionEvaluation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder, Advisor[] advisors) {
        this.chatClient = chatClientBuilder.defaultAdvisors(advisors).build();
    }

    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(String conversationId, Prompt prompt) {
        return chatClient.prompt(prompt)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId));
    }

    public Flux<String> stream(String conversationId, Prompt prompt) {
        return buildChatClientRequestSpec(conversationId, prompt).stream().content();
    }

    public ChatResponse call(String conversationId, Prompt prompt) {
        return buildChatClientRequestSpec(conversationId, prompt).call().chatResponse();
    }

    public EmotionEvaluation callEmotionEvaluation(String conversationId, Prompt prompt) {
        return buildChatClientRequestSpec(conversationId, prompt).call().entity(EmotionEvaluation.class);
    }

}
