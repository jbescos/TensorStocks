package com.jbescos.localbot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.client.ClientManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class WebSocket {

	private static final Logger LOGGER = Logger.getLogger(WebSocket.class.getName());
	
	public void start() throws DeploymentException, IOException, URISyntaxException {
		ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(new BookTickerMessageHandler());
				try {
					StringBuilder subscriptions = new StringBuilder();
					for (String symbol : Constants.SYMBOLS) {
						if (subscriptions.length() != 0) {
							subscriptions.append(",");
						}
						subscriptions.append("\"").append(symbol.toLowerCase()).append("@bookTicker\"");
					}
					String jsonFormat = "{\"method\": \"SUBSCRIBE\",\"params\":[" + subscriptions.toString() + "],\"id\": 1}";
					LOGGER.info(jsonFormat);
					session.getBasicRemote().sendText(jsonFormat);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, cec, new URI(Constants.WS_URL));
	}
	
	private static class BookTickerMessageHandler implements MessageHandler.Whole<String> {

		private static final Logger LOGGER = Logger.getLogger(BookTickerMessageHandler.class.getName());
		private final ObjectMapper mapper = new ObjectMapper();
		private final ConcurrentHashMap<String, Long> symbolTimestamps = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, Boolean> symbolNotWorking = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, SymbolWorker> symbolWorkers = new ConcurrentHashMap<>();
		private final Executor executor = Executors.newFixedThreadPool(Constants.WORKERS);

		@Override
		public void onMessage(String message) {
			try {
				Message obj = mapper.readValue(message, Message.class);
				final long now = System.currentTimeMillis();
				// It makes sure same symbol is not processed more than 1 time during the Constants.LATENCY
				long result = symbolTimestamps.compute(obj.s, (key, val) -> {
					if (val == null || (now - val > Constants.LATENCY)) {
						return now;
					}
					return val;
				});
				if (now == result) {
					SymbolWorker worker = symbolWorkers.computeIfAbsent(obj.s, k -> new SymbolWorker(k));
					// Only one thread can work at the same time for each symbol
					boolean notWorking = symbolNotWorking.compute(obj.s, (key, val) -> {
						return worker.startToWork();
					});
					if (notWorking) {
						executor.execute(() -> worker.process(obj));
					}
				}
			} catch (JsonProcessingException e) {
				LOGGER.warning("Couldn't parse " + message);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Unexpected error", e);
			}
		}
		
	}
	
	public static class Message {
		public long u;
		// Symbol
		public String s;
		// Selling price
		public String b;
		public String B;
		// Buying price
		public String a;
		public String A;
		@Override
		public String toString() {
			return "Message [" + s + ", Selling price = " + b + ", Buying price = " + a + "]";
		}
		
	}
}
