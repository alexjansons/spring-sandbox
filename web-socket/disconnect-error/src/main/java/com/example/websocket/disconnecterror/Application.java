package com.example.websocket.disconnecterror;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@SpringBootApplication
public class Application {

	private static class CustomErrorHandler extends StompSubProtocolErrorHandler {
		@Override
		public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);
			if (accessor == null || !accessor.isMutable()) {
				accessor = StompHeaderAccessor.wrap(errorMessage);
			}
			accessor.setHeader("stompCommand", StompCommand.ERROR);
			return this.handleInternal(accessor, errorMessage.getPayload(), null, null);
		}
	}

	@ConditionalOnProperty(name = "errorHandlerMode", havingValue = "ENABLED")
	@Bean(name = "errorHandler")
	public StompSubProtocolErrorHandler errorHandler() {
		return new StompSubProtocolErrorHandler();
	}

	@ConditionalOnProperty(name = "errorHandlerMode", havingValue = "CUSTOM_WITH_WORKAROUND")
	@Bean(name = "errorHandler")
	public StompSubProtocolErrorHandler customErrorHandler() {
		return new CustomErrorHandler();
	}

	@Configuration
	@EnableWebSocketMessageBroker
	static class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
		@Autowired(required = false)
		private StompSubProtocolErrorHandler errorHandler;

		private enum ErrorHandlerMode {
			ENABLED, DISABLED, CUSTOM_WITH_WORKAROUND
		}

		@Value("${errorHandlerMode}")
		private ErrorHandlerMode errorHandlerMode;

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/web-socket");
			if (errorHandlerMode != ErrorHandlerMode.DISABLED) {
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
