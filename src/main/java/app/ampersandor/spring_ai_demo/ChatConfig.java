package app.ampersandor.spring_ai_demo;

import app.ampersandor.spring_ai_demo.service.ChatService;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Scanner;

@Configuration
public class ChatConfig {
    // 디버깅 및 모니터링에 유용하며, 기본 로깅 포맷과 커스터마이징 기능을 지원
    // order 가 0 이므로 1을 주면 뒤에 실행 -1 을 주면 먼저실행
    // ChatModelCallAdvisor 같은경우는 Integer.MAX_VALUE 값으로 우선순위가 가장 낮음.
    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder().order(0).build();
    }

    // MessageWindowChatMemory를 사용해 최근 메시지를 유지하는 채팅 메모리
    // 초과 시 오래된 메시지를 순차적으로 제거하며, SystemMessage는 보관하지 않음
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                // .chatMemoryRepository(new InMemoryChatMemoryRepository())  이렇게 memoryRepository 를 지정할 수 있다.
                .maxMessages(10) // 최대 보관 메시지 수: 10
                .build();
    }

    // ChatClient 호출 전후에 chatMemory를 이용해 대화 내역을 프롬프트에 자동으로 주입하거나 응답 저장을 수행
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    // CLI 기능, SpringBoot 가 실행하자마자 CommandLineRunner 를 실행시킨다.
    // cli 가 true 일때만 실행된다.
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
                            .doFirst(() -> System.out.print("\nASSISTANT: "))
                            .doOnNext(System.out::print)
                            .doOnComplete(System.out::println)
                            .blockLast();
                }
            }
        };
    }
}
