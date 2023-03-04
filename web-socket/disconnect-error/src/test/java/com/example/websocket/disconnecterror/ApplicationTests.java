package com.example.websocket.disconnecterror;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTests {
	@LocalServerPort
	private Integer port;
	@Autowired
	private Application.WebSocketErrorHandler errorHandler;

	static class NoOpTaskScheduler implements TaskScheduler {

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
			return null;
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
			return null;
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
			return null;
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
			return null;
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
			return null;
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
			return null;
		}
	}


	@Test
	void disconnectByHeartBeatTask() throws Exception {
		String endpointUrl = String.format("ws://localhost:%d/web-socket", port);

		WebSocketStompClient webSocketClient = new WebSocketStompClient(new StandardWebSocketClient());
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean disconnectReceived = new AtomicBoolean(false);
		StompSessionHandlerAdapter dummySessionHandler = new StompSessionHandlerAdapter() {
			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				System.out.println("headers: " + headers);
				System.out.println("payload: " + payload);
				super.handleFrame(headers, payload);
				if (headers.containsKey("sessionAction")) {
					latch.countDown();
					disconnectReceived.set(true);
				}
			}

			@Override
			public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
				super.handleException(session, command, headers, payload, exception);
			}

			@Override
			public void handleTransportError(StompSession session, Throwable exception) {
				super.handleTransportError(session, exception);
				//???
				System.out.println("session: " + session);
				exception.printStackTrace();
				disconnectReceived.set(true);
			}
		};
		webSocketClient.setDefaultHeartbeat(new long[]{1000, 10000});
		// Emulate the case when the client does not send heart beats
		webSocketClient.setTaskScheduler(new NoOpTaskScheduler());

		StompSession session = webSocketClient.connectAsync(endpointUrl, dummySessionHandler).get();
		session.subscribe("/topic", dummySessionHandler);
		latch.await(10, TimeUnit.SECONDS);
		assertTrue(disconnectReceived.get());
	}
}
