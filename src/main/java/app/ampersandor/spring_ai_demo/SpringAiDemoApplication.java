package app.ampersandor.spring_ai_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiDemoApplication {

	public static void main(String[] args) {
		// Bootstraps the Spring context, auto-configuring Spring AI components declared in the config package.
		SpringApplication.run(SpringAiDemoApplication.class, args);
	}

}
