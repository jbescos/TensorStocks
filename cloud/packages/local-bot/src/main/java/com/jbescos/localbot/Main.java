package com.jbescos.localbot;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

import com.jbescos.localbot.WebSocket.KlineEvent;

import jakarta.websocket.DeploymentException;

public class Main {

	public static void main(String[] args) throws InterruptedException, DeploymentException, IOException, URISyntaxException {
		ConcurrentHashMap<String, BigDecimal> wallet = new ConcurrentHashMap<>();
		wallet.put(Constants.USDT, new BigDecimal("500"));
		Constants.SYMBOLS.stream().forEach(symbol -> wallet.put(symbol.toUpperCase().replace(Constants.USDT, ""), new BigDecimal(0)));
//		BookTickerMessageHandler<Message> handler = new BookTickerMessageHandler<>(Message.class, symbol -> new CsvWorker(symbol));
//		BookTickerMessageHandler<Message> handler = new BookTickerMessageHandler<>(Message.class, symbol -> new TraderWorker(symbol, wallet));
		BookTickerMessageHandler<KlineEvent> handler = new BookTickerMessageHandler<>(KlineEvent.class, symbol -> new KlineWorker());
//		WebSocket socket = new WebSocket(handler, WebSocket.SUBSCRIPTION_BOOK_TICKER);
		WebSocket socket = new WebSocket(handler, WebSocket.SUBSCRIPTION_KLINE);
		socket.start();
		Thread.sleep(Long.MAX_VALUE);
	}

}
