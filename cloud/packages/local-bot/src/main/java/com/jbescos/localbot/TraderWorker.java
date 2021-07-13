package com.jbescos.localbot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.jbescos.localbot.WebSocket.Message;

public class TraderWorker implements MessageWorker<Message> {

	private static final Logger LOGGER = Logger.getLogger(TraderWorker.class.getName());
	private final AtomicBoolean notWorking = new AtomicBoolean(true);
	private final String cryptoSymbol;
	private final String symbol;
	private final Map<String, BigDecimal> wallet;
	private MessageInternal first;
	private MessageInternal middle;
	private MessageInternal last;
	private MinMaxObject currentLimit;
	private MinMaxObject previousLimit;
	
	public TraderWorker(String symbol, Map<String, BigDecimal> wallet) {
		this.symbol = symbol.toUpperCase();
		this.cryptoSymbol = this.symbol.replace(Constants.USDT, "");
		this.wallet = wallet;
		LOGGER.info("TraderWorker instanced for " + symbol);
	}
	
	@Override
	public boolean startToWork() {
		return notWorking.compareAndSet(true, false);
	}
	
	@Override
	public void process(Message message, long now) {
		try {
			MessageInternal internal = new MessageInternal(message);
			last = middle;
			middle = first;
			first = internal;
			MinMaxObject minMax = evaluate(message);
			boolean changed = false;
			if (MinMax.MAX == minMax.minMax || MinMax.MIN == minMax.minMax) {
				previousLimit = currentLimit;
				currentLimit = minMax;
				changed = true;
			} else {
				changed = false;
			}
			if (changed && previousLimit != null && currentLimit != null) {
				if (previousLimit.minMax == MinMax.MAX && currentLimit.minMax == MinMax.MIN) {
					// MIN means it is time to buy. This applies commission.
					BigDecimal withCommission = currentLimit.price.multiply(Constants.COMMISSION_APPLIED);
//					BigDecimal withCommission = currentLimit.price;
					if (withCommission.compareTo(previousLimit.price) < 0) {
						LOGGER.info("Trying to buy -> Previous " + previousLimit + ", Current " + currentLimit + ", withCommission " + withCommission);
						BigDecimal usdtFromWallet =  wallet.get(Constants.USDT).multiply(Constants.AMOUNT_REDUCER);
						BigDecimal cryptosToBuy = usdtFromWallet.divide(withCommission, 8, RoundingMode.HALF_EVEN);
						if (usdtFromWallet.compareTo(Constants.MIN_BINANCE_USDT) > 0) {
							wallet.compute(cryptoSymbol, (key, value) -> value == null ? cryptosToBuy : value.add(cryptosToBuy));
							wallet.compute(Constants.USDT, (key, value) -> value.subtract(usdtFromWallet));
							LOGGER.info(" BUYING " + cryptosToBuy + " " + cryptoSymbol + " and spending " + usdtFromWallet + " USDT. " + currentLimit + " " + previousLimit);
							printWallet(currentLimit);
						}
					}
				} else if (previousLimit.minMax == MinMax.MIN && currentLimit.minMax == MinMax.MAX) {
					if (currentLimit.price.compareTo(previousLimit.price) > 0) {
						LOGGER.info("Trying to sell -> Previous " + previousLimit + ", Current " + currentLimit);
						// FIXME Use compute instead of get
						BigDecimal cryptosFromWallet = wallet.get(cryptoSymbol).multiply(Constants.AMOUNT_REDUCER);
						BigDecimal usdtToSell = cryptosFromWallet.multiply(currentLimit.price);
						if (usdtToSell.compareTo(Constants.MIN_BINANCE_USDT) > 0) {
							wallet.compute(cryptoSymbol, (key, value) -> value.subtract(cryptosFromWallet));
							wallet.compute(Constants.USDT, (key, value) -> value == null ? value : value.add(usdtToSell));
							LOGGER.info(" SELLING " + cryptosFromWallet + " " + cryptoSymbol + " and getting " + usdtToSell + " USDT. " + currentLimit + " " + previousLimit);
							printWallet(currentLimit);
						}
					}
				}
			}
		} finally {
			notWorking.set(true);
		}
	}
	
	private void printWallet(MinMaxObject currentLimit) {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, BigDecimal> entry : wallet.entrySet()) {
			builder.append("\n").append(entry.getKey()).append(": ").append(entry.getValue());
		}
		LOGGER.info("Wallet: " + wallet);
	}
	
	private MinMaxObject evaluate(Message message) {
		if (first != null && middle != null && last != null) {
			if (middle.sellingPrice.compareTo(first.sellingPrice) > 0 && middle.sellingPrice.compareTo(last.sellingPrice) > 0) {
				// Check that new price is higher than the previous
				if (previousLimit == null || (previousLimit != null && first.sellingPrice.compareTo(previousLimit.price) > 0)) {
					// Note the price is the first because it is the current value, not the middle
					MinMaxObject max = new MinMaxObject(MinMax.MAX, new BigDecimal(message.b));
//					LOGGER.info("Found " + max + ". The real MAX was " + middle.sellingPrice);
					return max;
				}
			} else if (middle.buyingPrice.compareTo(first.buyingPrice) < 0 && middle.buyingPrice.compareTo(last.buyingPrice) < 0) {
				if (previousLimit == null || (previousLimit != null && first.buyingPrice.compareTo(previousLimit.price) < 0)) {
					// Note the price is the first because it is the current value, not the middle
					MinMaxObject min = new MinMaxObject(MinMax.MIN, new BigDecimal(message.a));
//					LOGGER.info("Found " + min + ". The real MIN was " + middle.buyingPrice);
					return min;
				}
			}
		}
		return new MinMaxObject(MinMax.NOTHING, null);
	}
	
	private static enum MinMax {
		MIN, MAX, NOTHING;
	}
	
	private class MinMaxObject {
		private final MinMax minMax;
		private final BigDecimal price;
		public MinMaxObject(MinMax minMax, BigDecimal price) {
			this.minMax = minMax;
			this.price = price;
		}
		@Override
		public String toString() {
			return symbol + " [" + minMax + ", price=" + price + "]";
		}
	}
	
	private class MessageInternal {
		private final BigDecimal buyingPrice;
		private final BigDecimal sellingPrice;
		public MessageInternal(Message message) {
			if (first == null) {
				buyingPrice = Constants.ewma(new BigDecimal(message.a), null);
				sellingPrice = Constants.ewma(new BigDecimal(message.b), null);
			} else {
				buyingPrice = Constants.ewma(new BigDecimal(message.a), first.buyingPrice);
				sellingPrice = Constants.ewma(new BigDecimal(message.b), first.sellingPrice);
			}
		}
	}
}
