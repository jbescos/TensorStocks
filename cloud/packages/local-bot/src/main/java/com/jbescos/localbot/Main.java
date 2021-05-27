package com.jbescos.localbot;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.DeploymentException;

public class Main {

	public static void main(String[] args) throws InterruptedException, DeploymentException, IOException, URISyntaxException {
		ConcurrentHashMap<String, BigDecimal> wallet = new ConcurrentHashMap<>();
		wallet.put(Constants.USDT, new BigDecimal("500"));
		Constants.SYMBOLS.stream().forEach(symbol -> wallet.put(symbol.toUpperCase().replace(Constants.USDT, ""), new BigDecimal(0)));
		BookTickerMessageHandler handler = new BookTickerMessageHandler(wallet);
		WebSocket socket = new WebSocket(handler);
		socket.start();
		Thread.sleep(Long.MAX_VALUE);
	}

}
