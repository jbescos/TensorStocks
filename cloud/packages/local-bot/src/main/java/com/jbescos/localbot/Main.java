package com.jbescos.localbot;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

import com.jbescos.localbot.WebSocket.KlineEvent;
import com.jbescos.localbot.WebSocket.Message;

import jakarta.websocket.DeploymentException;

public class Main {

	public static void main(String[] args) throws InterruptedException, DeploymentException, IOException, URISyntaxException {
		ConcurrentHashMap<String, String> prices = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, BigDecimal> wallet = new ConcurrentHashMap<>();
		wallet.put(Constants.USDT, new BigDecimal("500"));
		Constants.SYMBOLS.stream().forEach(symbol -> wallet.put(symbol.toUpperCase().replace(Constants.USDT, ""), new BigDecimal(0)));
		WebSocket tickerSocket = new WebSocket(new MessageHandlerImpl<>(Message.class, symbol -> new PricesWorker(symbol, prices)), WebSocket.SUBSCRIPTION_BOOK_TICKER);
		tickerSocket.start();
		WebSocket klineSocket = new WebSocket(new MessageHandlerImpl<>(KlineEvent.class, symbol -> new KlineWorker(symbol, prices)), WebSocket.SUBSCRIPTION_KLINE);
		klineSocket.start();
		Thread.sleep(Long.MAX_VALUE);
	}

}
