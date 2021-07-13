package com.jbescos.localbot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jbescos.localbot.WebSocket.Message;

public class PricesWorker implements MessageWorker<Message> {

	private final AtomicBoolean notWorking = new AtomicBoolean(true);
	private final String symbol;
	private final ConcurrentHashMap<String, String> prices;
	
	public PricesWorker(String symbol, ConcurrentHashMap<String, String> prices) {
		this.symbol = symbol;
		this.prices = prices;
	}

	@Override
	public boolean startToWork() {
		return notWorking.compareAndSet(true, false);
	}

	@Override
	public void process(Message message, long now) {
		try {
			prices.put(symbol, message.a);
		} finally {
			notWorking.set(true);
		}
	}

}
