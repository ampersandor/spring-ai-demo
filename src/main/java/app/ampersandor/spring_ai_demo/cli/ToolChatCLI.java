package app.ampersandor.spring_ai_demo.cli;

import app.ampersandor.spring_ai_demo.service.ToolChatService;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Scanner;

@Configuration
@ConditionalOnProperty(name = "app.mode", havingValue = "tool")
public class ToolChatCLI {

    @ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true")
    @Bean
    public CommandLineRunner toolCLI(@Value("${spring.application.name}") String applicationName,
            ToolChatService toolChatService) {
        return args -> {

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger("ROOT").detachAppender("CONSOLE");

            System.out.println("\n" + applicationName + " CLI bot");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nUSER: ");
                    String userMessage = scanner.nextLine();
                    toolChatService.stream("cli", Prompt.builder().content(userMessage).build())
                            .doFirst(() -> System.out.print("\nASSISTANT: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .doOnError(System.err::println)
                            .blockLast();
                }
            }
        };
    }

}
