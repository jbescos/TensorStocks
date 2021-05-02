package es.tododev.stocks.binance;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

public class BinanceBot {

	private static final long SLEEP_MILLIS = 1000 * 60;
	private final OperatorAPI api;

	public BinanceBot(OperatorAPI api) {
		this.api = api;
	}

	public void run() throws InterruptedException {
		System.out.println("Bot running");
		while (true) {
			exchangeInfo();
			Thread.sleep(SLEEP_MILLIS);
		}
	}

	private void exchangeInfo() {
		List<Price> prices = api.request("/api/v3/ticker/price", null, false, false, new GenericType<List<Price>>() {});
		prices = prices.stream().filter(price -> price.getSymbol().endsWith("USDT")).collect(Collectors.toList());
		// TODO Append in a CSV?
		System.out.println(prices);
	}
}
