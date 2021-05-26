package com.jbescos.localbot;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.jbescos.localbot.WebSocket.Message;

public class SymbolWorker {

	private static final Logger LOGGER = Logger.getLogger(SymbolWorker.class.getName());
	private final AtomicBoolean notWorking = new AtomicBoolean(true);
	private final String symbol;
	
	public SymbolWorker(String symbol) {
		this.symbol = symbol;
	}
	
	public boolean startToWork() {
		return notWorking.compareAndSet(true, false);
	}
	
	public void process(Message message) {
		LOGGER.info(Thread.currentThread().getName() + " Worker " + symbol + " is working: " + message);

		LOGGER.info(Thread.currentThread().getName() + " Worker " + symbol + " finished!");
		notWorking.set(true);
	}
}
