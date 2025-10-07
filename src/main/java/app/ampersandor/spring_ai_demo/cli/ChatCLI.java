package app.ampersandor.spring_ai_demo.cli;

import app.ampersandor.spring_ai_demo.service.ChatService;
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
public class ChatCLI {

    /**
     * Provides a simple terminal experience for chatting with the configured model.
     * The bean is only created when {@code spring.application.cli=true}, letting you opt-in via properties.
     */
    @ConditionalOnProperty(prefix = "spring.application", name = "cli", havingValue = "true")
    @Bean
    public CommandLineRunner cli(@Value("${spring.application.name}") String applicationName, ChatService chatService) {
        return args -> {

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " CLI bot");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUSER: ");
                    String userMessage = scanner.nextLine();
                    chatService.stream("cli", Prompt.builder().content(userMessage).build())
                            // doFirst/doOn* are Reactor lifecycle hooks that let us print alongside the stream.
                            .doFirst(() -> System.out.print("\nASSISTANT: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .blockLast();
                }
            }
        };
    }



}
