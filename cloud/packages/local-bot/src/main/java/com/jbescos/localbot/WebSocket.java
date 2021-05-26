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
	private final MessageHandler.Whole<String> messageHandler;
	
	public WebSocket(MessageHandler.Whole<String> messageHandler) {
		this.messageHandler = messageHandler;
	}
	
	public void start() throws DeploymentException, IOException, URISyntaxException {
		ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(messageHandler);
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
