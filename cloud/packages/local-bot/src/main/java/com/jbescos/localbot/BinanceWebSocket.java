package com.jbescos.localbot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.client.ClientManager;

import com.jbescos.localbot.PricesWorker.Price;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class BinanceWebSocket implements IWebsocket {

	private static final Logger LOGGER = Logger.getLogger(BinanceWebSocket.class.getName());
	public static final String SUBSCRIPTION_BOOK_TICKER = "@bookTicker";
	private final MessageHandler.Whole<String> messageHandler;
	private final String subscription;
	
	public BinanceWebSocket(MessageHandler.Whole<String> messageHandler, String subscription) {
		this.messageHandler = messageHandler;
		this.subscription = subscription;
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
						subscriptions.append("\"").append(symbol.toLowerCase()).append(subscription + "\"");
					}
					String jsonFormat = "{\"method\": \"SUBSCRIBE\",\"params\":[" + subscriptions.toString() + "],\"id\": 1}";
					LOGGER.info(() -> jsonFormat);
					session.getBasicRemote().sendText(jsonFormat);
					LOGGER.info("BinanceWebSocket started");
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Unexpected error", e);
				}
			}
		}, cec, new URI(Constants.BINANCE_WS_URL));
	}
	
	public static class BinanceMessage implements Priceable {
		public long u;
		// Symbol
		public String s;
		// Selling price -> best bid price
		public String b;
		public String B;
		// Buying price -> best ask price
		public String a;
		public String A;
		@Override
		public String toString() {
			return "Message [" + s + ", Selling price = " + b + ", Buying price = " + a + "]";
		}
		@Override
		public String symbol() {
			return s;
		}
		@Override
		public Price toPrice() {
			return new Price(a, b, s);
		}
	}
}
