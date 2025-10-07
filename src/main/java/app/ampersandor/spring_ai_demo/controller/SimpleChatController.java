package app.ampersandor.spring_ai_demo.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class SimpleChatController {

    private final ChatClient chatClient;

    public SimpleChatController(ChatClient.Builder chatClientBuilder) {
        // The builder encapsulates all auto-configured settings (model, API key, advisors).
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Simplest possible REST endpoint that forwards the query straight to the model.
     * Options can be set fluently; here we raise the temperature to encourage creative answers.
     */
    @GetMapping("/ai")
    String generation(String userPrompt) {
        return this.chatClient.prompt()
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(1.0).build())
                .call()
                .content();
    }

    /**
     * Token-by-token streaming variant of {@link #generation(String)}.
     * The SSE media type instructs Spring WebFlux to keep the HTTP connection open.
     */
    @GetMapping(value="/stream", produces= MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(String userPrompt) {
        return this.chatClient.prompt()
                .user(userPrompt)
                .stream()
                .content();
    }
}
