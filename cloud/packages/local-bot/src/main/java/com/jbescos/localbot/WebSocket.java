package com.jbescos.localbot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
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

	public void start() throws DeploymentException, IOException, URISyntaxException {
		ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(new BookTickerMessageHandler());
				try {
					StringBuilder subscriptions = new StringBuilder();
					for (String symbol : ConfigProperties.SYMBOLS) {
						if (subscriptions.length() != 0) {
							subscriptions.append(",");
						}
						subscriptions.append("\"").append(symbol.toLowerCase()).append("@bookTicker\"");
					}
					String jsonFormat = "{\"method\": \"SUBSCRIBE\",\"params\":[" + subscriptions.toString() + "],\"id\": 1}";
					System.out.println(jsonFormat);
					session.getBasicRemote().sendText(jsonFormat);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, cec, new URI(ConfigProperties.WS_URL));
	}
	
	private static class BookTickerMessageHandler implements MessageHandler.Whole<String> {

		private static final Logger LOGGER = Logger.getLogger(BookTickerMessageHandler.class.getName());
		private final ObjectMapper mapper = new ObjectMapper();
		private final ConcurrentHashMap<String, Long> symbolTimestamps = new ConcurrentHashMap<>();

		@Override
		public void onMessage(String message) {
			try {
				Message obj = mapper.readValue(message, Message.class);
				final long now = System.currentTimeMillis();
				long result = symbolTimestamps.compute(obj.s, (key, val) -> {
					if ((now - val > ConfigProperties.LATENCY) || val == null) {
						return now;
					}
					return val;
				});
				if (now == result) {
					// TODO
				}
			} catch (JsonProcessingException e) {
				LOGGER.warning("Couldn't parse " + message);
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
	}
}
