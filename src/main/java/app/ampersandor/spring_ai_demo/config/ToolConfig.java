package app.ampersandor.spring_ai_demo.config;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.mode", havingValue = "tool")
public class ToolConfig {

    @Bean
    public ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder().build();
    }

    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        // alwaysThrow를 true로 설정: 예외를 상위(chatClient 사용 Service)로 던짐
        // false(기본값): 예외를 AI 모델에 메시지로 전달
        return new DefaultToolExecutionExceptionProcessor(false);
    }

}