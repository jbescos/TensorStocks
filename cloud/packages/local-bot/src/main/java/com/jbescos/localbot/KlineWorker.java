package com.jbescos.localbot;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.jbescos.localbot.WebSocket.KlineEvent;

public class KlineWorker implements MessageWorker<KlineEvent> {

	private static final Logger LOGGER = Logger.getLogger(KlineWorker.class.getName());
	private final AtomicBoolean notWorking = new AtomicBoolean(true);
	private KlineEvent previous = new KlineEvent();

	@Override
	public boolean startToWork() {
		return notWorking.compareAndSet(true, false);
	}

	@Override
	public void process(KlineEvent message, long now) {
		try {
			if (!message.k.equals(previous.k)) {
				LOGGER.info("Received: " + message);
				previous = message;
			}
		} finally {
			notWorking.set(true);
		}
	}

}
