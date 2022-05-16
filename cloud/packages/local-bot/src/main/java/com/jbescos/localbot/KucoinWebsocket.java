package com.jbescos.localbot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import org.glassfish.tyrus.client.ClientManager;

import com.jbescos.localbot.KucoinRestAPI.WebsocketInfo;
import com.jbescos.localbot.PricesWorker.Price;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class KucoinWebsocket implements IWebsocket {

	private static final Logger LOGGER = Logger.getLogger(KucoinWebsocket.class.getName());
	private final MessageHandler.Whole<String> messageHandler;
	private final Client client;
	private final String connectId = "KucoinWebsocketBotlogic";
	private final ScheduledExecutorService keepAlive = Executors.newSingleThreadScheduledExecutor();
	
	public KucoinWebsocket(MessageHandler.Whole<String> messageHandler, Client client) {
		this.messageHandler = messageHandler;
		this.client = client;
	}

	@Override
	public void start() throws DeploymentException, IOException, URISyntaxException {
		KucoinRestAPI api = new KucoinRestAPI(client);
		WebsocketInfo websocketInfo = api.websocketInfo();
		LOGGER.log(Level.INFO, "Kucoin Websocket info " + websocketInfo);
		ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		Session session = client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				session.addMessageHandler(messageHandler);
				try {
					Map<String, Object> subscribe = new HashMap<>();
					subscribe.put("id", System.currentTimeMillis());
					subscribe.put("type", "subscribe");
					subscribe.put("privateChannel", false);
					subscribe.put("response", false);
					StringBuilder subscriptions = new StringBuilder();
					for (String symbol : Constants.SYMBOLS) {
						if (subscriptions.length() != 0) {
							subscriptions.append(",");
						}
						String sym = symbol.replaceFirst(Constants.USDT, "");
						subscriptions.append(sym).append("-").append(Constants.USDT);
					}
					subscribe.put("topic", "/market/ticker:" + subscriptions.toString());
					String jsonFormat = Constants.MAPPER.writeValueAsString(subscribe);
					LOGGER.info(() -> jsonFormat);
					session.getBasicRemote().sendText(jsonFormat);
					LOGGER.info("KucoinWebsocket started");
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Unexpected error", e);
				}
			}
		}, cec, new URI(websocketInfo.getEndpoint() + "?token=" + websocketInfo.getToken() + "&[connectId=" + connectId + "]"));
		keepAlive.scheduleAtFixedRate(() -> {
			try {
				session.getBasicRemote().sendText("{\"id\":\"" + System.currentTimeMillis() + "\",\"type\":\"ping\"}");
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Cannot ping", e);
			}
		}, 0, websocketInfo.getPingInterval() - 100, TimeUnit.MILLISECONDS);
	}

	public static class KucoinMessage implements Priceable {

		private String type;
		private String topic;
		private String subject;
		private String symbol;
		private Map<String, String> data;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getTopic() {
			return topic;
		}
		public void setTopic(String topic) {
			this.topic = topic;
		}
		public String getSubject() {
			return subject;
		}
		public void setSubject(String subject) {
			this.subject = subject;
		}
		public Map<String, String> getData() {
			return data;
		}
		public void setData(Map<String, String> data) {
			this.data = data;
		}
		@Override
		public String symbol() {
			if (symbol == null) {
				int startSymbol = topic.lastIndexOf(":") + 1;
				symbol = topic.substring(startSymbol, topic.length()).replaceAll("-", "");
			}
			return symbol;
		}
		@Override
		public Price toPrice() {
			return new Price(data.get("bestAsk"), data.get("bestBid"), symbol());
		}
	}
}
