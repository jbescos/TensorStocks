package com.jbescos.localbot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.glassfish.tyrus.client.ClientManager;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class WebSocket {

	// wss://stream.binance.com:9443/ws/<symbol>@bookTicker
	// wss://stream.binance.com:9443/ws/stream?streams=<streamName1>/<streamName2>/<streamName3>
	private final String url;

	public WebSocket(String url) {
		this.url = url;
	}

	public void start() throws DeploymentException, IOException, URISyntaxException {
		ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				try {
					session.addMessageHandler(new MessageHandler.Whole<String>() {
						@Override
						public void onMessage(String message) {
							System.out.println("Received message: " + message);
						}
					});
					session.getBasicRemote().sendText("test");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, cec, new URI(url));
	}
}
