package app.ampersandor.spring_ai_demo.cli;

import app.ampersandor.spring_ai_demo.service.RagChatService;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;

@Configuration
public class RagChatCLI {

    /**
     * CLI variant that routes prompts through the RAG pipeline.
     * Enabled via {@code app.cli.enabled=true}. A default filter expression can also be injected from properties.
     */
    @ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true")
    @Bean
    public CommandLineRunner cli(@Value("${spring.application.name}") String applicationName,
                                 RagChatService ragChatService,
                                 @Value("${app.cli.filter-expression:}") String filterExpression) {
        return args -> {

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " CLI bot");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUSER: ");
                    String userMessage = scanner.nextLine();
                    ragChatService.stream("cli", Prompt.builder().content(userMessage).build(),
                                    // filterExpression 을 Optional로 전달
                                    Optional.of(filterExpression).filter(Predicate.not(String::isBlank)))
                            // RAG responses are also streamed token-by-token.
                            .doFirst(() -> System.out.print("\nASSISTANT: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .blockLast();
                }
            }
        };
    }
}
