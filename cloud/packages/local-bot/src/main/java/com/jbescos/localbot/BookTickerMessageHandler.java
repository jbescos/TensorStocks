package com.jbescos.localbot;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbescos.localbot.WebSocket.Message;

import jakarta.websocket.MessageHandler;

public class BookTickerMessageHandler implements MessageHandler.Whole<String> {

	private static final Logger LOGGER = Logger.getLogger(BookTickerMessageHandler.class.getName());
	private final ObjectMapper mapper = new ObjectMapper();
	private final ConcurrentHashMap<String, Long> symbolTimestamps = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Boolean> symbolNotWorking = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, SymbolWorker> symbolWorkers = new ConcurrentHashMap<>();
	private final Executor executor = Executors.newFixedThreadPool(Constants.WORKERS);
	private final ConcurrentHashMap<String, BigDecimal> wallet;

	public BookTickerMessageHandler(ConcurrentHashMap<String, BigDecimal> wallet) {
		this.wallet = wallet;
	}
	
	@Override
	public void onMessage(String message) {
		try {
			Message obj = mapper.readValue(message, Message.class);
			final long now = System.currentTimeMillis();
			// It makes sure same symbol is not processed more than 1 time during the Constants.LATENCY
			long result = symbolTimestamps.compute(obj.s, (key, val) -> {
				if (val == null || (now - val > Constants.LATENCY)) {
					return now;
				}
				return val;
			});
			if (now == result) {
				SymbolWorker worker = symbolWorkers.computeIfAbsent(obj.s, k -> new SymbolWorker(k, wallet));
				// Only one thread can work at the same time for each symbol
				boolean notWorking = symbolNotWorking.compute(obj.s, (key, val) -> {
					return worker.startToWork();
				});
				if (notWorking) {
					executor.execute(() -> worker.process(obj));
				}
			}
		} catch (JsonProcessingException e) {
			LOGGER.warning("Couldn't parse " + message);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unexpected error", e);
		}
	}
	
}
