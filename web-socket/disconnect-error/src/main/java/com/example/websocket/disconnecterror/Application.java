package com.example.websocket.disconnecterror;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class Application {

	@Configuration
	@EnableWebSocketMessageBroker
	static class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
		@Autowired
		private StompSubProtocolErrorHandler errorHandler;

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.setErrorHandler(errorHandler)
					.addEndpoint("/web-socket");
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry config) {
			config.setApplicationDestinationPrefixes("/app");
			config.enableSimpleBroker("/topic")
					.setHeartbeatValue(new long[]{10000, 1000})
					.setTaskScheduler(new DefaultManagedTaskScheduler());
		}
	}

	@Component
	static class WebSocketErrorHandler extends StompSubProtocolErrorHandler {
		@Override
		public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
			try {
				Message<byte[]> message = handleErrorMessageToClientFixed(errorMessage);
				return message;
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}

		public Message<byte[]> handleErrorMessageToClientFixed(Message<byte[]> errorMessage) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);
			if (accessor == null || !accessor.isMutable()) {
				accessor = StompHeaderAccessor.wrap(errorMessage);
				accessor.addNativeHeader("sessionAction", "DISCONNECT");
				StompCommand stompCommand = accessor.getCommand();
				if (stompCommand == null) {
//					accessor.setHeader("stompCommand", StompCommand.ERROR);
				}
			}
			return handleInternal(accessor, errorMessage.getPayload(), null, null);
		}


	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
