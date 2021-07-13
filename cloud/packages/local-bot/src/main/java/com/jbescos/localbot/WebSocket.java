package com.jbescos.localbot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.client.ClientManager;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class WebSocket {

	private static final Logger LOGGER = Logger.getLogger(WebSocket.class.getName());
	public static final String SUBSCRIPTION_BOOK_TICKER = "@bookTicker";
	public static final String SUBSCRIPTION_KLINE = "@kline_1m";
	private final MessageHandler.Whole<String> messageHandler;
	private final String subscription;
	
	public WebSocket(MessageHandler.Whole<String> messageHandler, String subscription) {
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
					LOGGER.info(jsonFormat);
					session.getBasicRemote().sendText(jsonFormat);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Unexpected error", e);
				}
			}
		}, cec, new URI(Constants.WS_URL));
	}
	
	public static interface Symbolable {
		String symbol();
	}
	
	public static class Message implements Symbolable {
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
		@Override
		public String symbol() {
			return s;
		}
	}
	
	public static class KlineEvent implements Symbolable {
		// Event type
		public String e;
		// Event time
		public long E;
		// Symbol
		public String s;
		public Kline k;
		@Override
		public String symbol() {
			return s;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Symbol=").append(s).append(", EventType=").append(e).append(", EventTime=").append(Constants.format(new Date(E))).append(", KLine=[").append(k).append("]");
			return builder.toString();
		}
	}
	
	public static class Kline implements Symbolable {
		// Kline start time
		public long t;
		// Kline close time
		public long T;
		// Symbol
		public String s;
		// Interval
		public String i;
		// First trade ID
		public int f;
		// Last trade ID
		public int L;
		// Open price
		public String o;
		// Close price
		public String c;
		// High price
		public String h;
		// Low price
		public String l;
		// Base asset volume
		public String v;
		// Number of trades
		public int n;
		// Is this kline closed?
		public boolean x;
		// Quote asset volume
		public String q;
		// Taker buy base asset volume
		public String V;
		// Taker buy quote asset volume
		public String Q;
		// Ignore
		public String B;
		@Override
		public String symbol() {
			return s;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("StartTime=").append(Constants.format(new Date(t))).append(", CloseTime=").append(Constants.format(new Date(T))).append(", Interval=").append(i)
			.append(", OpenPrice=").append(o).append(", ClosePrice=").append(c).append(", HighPrice=").append(h).append(", LowPrice=").append(l)
			.append(", BaseAssetVolume=").append(v).append(", NumberOfTrades=").append(n).append(", AssetVolume=").append(q).append(", TakerBuyBaseAssetVolume=").append(V)
			.append(", TakerBuyQuoteAssetVolume=").append(Q).append(", Ignore=").append(B).append(", Closed=").append(x);
			return builder.toString();
		}
	}
}
