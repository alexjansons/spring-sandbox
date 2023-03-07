package com.example.websocket.disconnecterror;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@SpringBootApplication
public class Application {

	@Bean
	public StompSubProtocolErrorHandler errorHandler() {
		return new StompSubProtocolErrorHandler();
	}

	@Configuration
	@EnableWebSocketMessageBroker
	static class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
		@Autowired
		private StompSubProtocolErrorHandler errorHandler;

		@Value("${errorHandler.enabled}")
		private Boolean errorHandlerEnabled;

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/web-socket");
			if (errorHandlerEnabled) {
				registry.setErrorHandler(errorHandler);
			}
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry config) {
			config.setApplicationDestinationPrefixes("/app");
			config.enableSimpleBroker("/topic")
					.setHeartbeatValue(new long[]{10000, 1000})
					.setTaskScheduler(new DefaultManagedTaskScheduler());
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
