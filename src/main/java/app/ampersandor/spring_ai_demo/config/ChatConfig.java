package app.ampersandor.spring_ai_demo.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {
    /**
     * Registers a chat advisor that prints every request/response pair.
     * Advisors run in ascending {@code order}; lower numbers run first.
     * This logger is placed at 0 so it runs before user-defined advisors
     * but after any negative-order interceptors you may later introduce.
     */
    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder().order(0).build();
    }

    /**
     * Stores the last N exchanges for each conversation id.
     * Spring AI will call {@link ChatMemory} between requests to keep context.
     * {@link MessageWindowChatMemory} simply evicts the oldest messages when the window size is exceeded.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                // A different ChatMemoryRepository (e.g. Redis) can be plugged in here if persistence is required.
                .maxMessages(10)
                .build();
    }

    /**
     * Injects chat history into prompts and captures model replies back into {@link ChatMemory}.
     * The advisor pulls the conversation id from the request metadata (see ChatService).
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }


}
