package com.example.websocket.disconnecterror;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTests {
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

	void testDisconnectByHeartBeatTask(int port) throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicBoolean disconnectReceived = new AtomicBoolean(false);
		String endpointUrl = String.format("ws://localhost:%d/web-socket", port);
		WebSocketStompClient webSocketClient = new WebSocketStompClient(new StandardWebSocketClient());
		StompSessionHandlerAdapter dummySessionHandler = new StompSessionHandlerAdapter() {
			@Override
			public void handleTransportError(StompSession session, Throwable exception) {
				super.handleTransportError(session, exception);
				assertTrue(exception instanceof ConnectionLostException);
				exception.printStackTrace();
				countDownLatch.countDown();
				disconnectReceived.set(true);
			}
		};
		webSocketClient.setDefaultHeartbeat(new long[]{1000, 10000});
		// Emulate the client not sending ping requests (it happens in some cases when computer is locked e.g.)
		webSocketClient.setTaskScheduler(new NoOpTaskScheduler());
		StompSession session = webSocketClient.connectAsync(endpointUrl, dummySessionHandler).get();
		session.subscribe("/topic", dummySessionHandler);
		countDownLatch.await(10, TimeUnit.SECONDS);
		assertTrue(disconnectReceived.get());
	}

	// This test fails with IllegalArgumentException: No StompHeaderAccessor
	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	@DirtiesContext
	@TestPropertySource(properties = {"errorHandlerMode=ENABLED"})
	class DisconnectWithErrorHandler {
		@LocalServerPort
		Integer port;

		@Test
		void test() throws Exception {
			testDisconnectByHeartBeatTask(port);
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	@DirtiesContext
	@TestPropertySource(properties = {"errorHandlerMode=DISABLED"})
	class DisconnectWithoutErrorHandler {
		@LocalServerPort
		Integer port;

		@Test
		void test() throws Exception {
			testDisconnectByHeartBeatTask(port);
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	@DirtiesContext
	@TestPropertySource(properties = {"errorHandlerMode=CUSTOM_WITH_WORKAROUND"})
	class DisconnectWithCustomErrorHandlerWithWorkaround {
		@LocalServerPort
		Integer port;

		@Test
		void test() throws Exception {
			testDisconnectByHeartBeatTask(port);
		}
	}
}
