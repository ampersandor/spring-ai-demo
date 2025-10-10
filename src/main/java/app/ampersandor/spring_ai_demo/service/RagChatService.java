package app.ampersandor.spring_ai_demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "rag")
public class RagChatService {

    private final ChatClient chatClient;

    public RagChatService(ChatClient.Builder chatClientBuilder, Advisor[] advisors) {
        // RAG 에서는 검색된 내용을 기반으로 정확한 정보를 생성해야 하므로 생성 다양성을 줄이기 위해 temperature를 0.0으로 설정
        this.chatClient = chatClientBuilder.defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .defaultAdvisors(advisors).build();
    }

    /**
     * Adds the conversation id parameter so memory advisors can replay history.
     * When a filter expression is present we forward it to {@link VectorStoreDocumentRetriever}
     * so that only matching documents are retrieved during augmentation.
     */
    private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(String conversationId, Prompt prompt,
            Optional<String> filterExpressionAsOpt) {
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt(prompt)
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId));
        // filterExpression 이 있을 경우 VectorStore 검색 필터 설정
        filterExpressionAsOpt.ifPresent(filterExpression -> chatClientRequestSpec.advisors(
                advisors -> advisors.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression)));
        return chatClientRequestSpec;
    }

    /**
     * Streams RAG answers with the optional document filter applied.
     */
    public Flux<String> stream(String conversationId, Prompt prompt, Optional<String> filterExpressionAsOpt) {
        return buildChatClientRequestSpec(conversationId, prompt, filterExpressionAsOpt).stream().content();
    }

    /**
     * Blocking RAG call that returns the full {@link ChatResponse}.
     * call() 함수 내부에 들어가보면 advisor 호출을 찾을 수 있다.
     */
    public ChatResponse call(String conversationId, Prompt prompt, Optional<String> filterExpressionAsOpt) {
        return buildChatClientRequestSpec(conversationId, prompt, filterExpressionAsOpt).call().chatResponse();
    }

}
