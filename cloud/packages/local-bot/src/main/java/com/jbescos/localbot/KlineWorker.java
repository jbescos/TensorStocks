package com.jbescos.localbot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.jbescos.localbot.WebSocket.KlineEvent;

public class KlineWorker implements MessageWorker<KlineEvent> {

	private static final Logger LOGGER = Logger.getLogger(KlineWorker.class.getName());
	private final AtomicBoolean notWorking = new AtomicBoolean(true);
	private final String symbol;
	private final ConcurrentHashMap<String, String> prices;
	private long startTime = 0;
	private BigDecimal previousVolumeTotal = new BigDecimal(0);
	private BigDecimal previousBuyVolumeTotal = new BigDecimal(0);

	public KlineWorker(String symbol, ConcurrentHashMap<String, String> prices) {
		this.prices = prices;
		this.symbol = symbol;
	}

	@Override
	public boolean startToWork() {
		return notWorking.compareAndSet(true, false);
	}

	@Override
	public void process(KlineEvent message, long now) {
		try {
//			LOGGER.info("Received: " + message);
			BigDecimal volume = null;
			BigDecimal buyVolume = null;
			BigDecimal volumeTotal = new BigDecimal(message.k.v);
			BigDecimal buyVolumeTotal = new BigDecimal(message.k.V);
			if (startTime != message.k.t) {
				LOGGER.info("New Kline: " + message);
				startTime = message.k.t;
				volume = volumeTotal;
				buyVolume = buyVolumeTotal;
				
			} else {
				volume = volumeTotal.subtract(previousVolumeTotal);
				buyVolume = buyVolumeTotal.subtract(previousBuyVolumeTotal);
			}
			BigDecimal normalizedBuy = null;
			if (volume.equals(new BigDecimal(0))) {
				normalizedBuy = volume;
			} else {
				normalizedBuy = buyVolume.divide(volume, 8, RoundingMode.HALF_EVEN);
			}
			BigDecimal normalizedSell = new BigDecimal(1).subtract(normalizedBuy);
			LOGGER.info("Buy: " + Constants.format(normalizedBuy) + " Sell: " + Constants.format(normalizedSell) + " Price: " + prices.get(symbol));
			previousVolumeTotal = volumeTotal;
			previousBuyVolumeTotal = buyVolumeTotal;
		} finally {
			notWorking.set(true);
		}
	}

}
