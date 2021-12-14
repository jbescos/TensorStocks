package com.jbescos.common;

import java.util.logging.Logger;

public class PanicBroker implements Broker {

	private static final Logger LOGGER = Logger.getLogger(PanicBroker.class.getName());
	private final String symbol;
	private final CsvRow newest;
	private final TransactionsSummary summary;

	public PanicBroker(String symbol, CsvRow newest, TransactionsSummary summary) {
		this.symbol = symbol;
		this.newest = newest;
		this.summary = summary;
		LOGGER.warning(() -> symbol + " selling because of panic. Current price is " + Utils.format(newest.getPrice()) + " USDT and the min profitable is " +  Utils.format(summary.getMinProfitable()) + " USDT");
	}

	@Override
	public Action getAction() {
		return Action.SELL;
	}

	@Override
	public CsvRow getNewest() {
		return newest;
	}

	@Override
	public String getSymbol() {
		return symbol;
	}

	@Override
	public double getFactor() {
		return 0.999;
	}

	@Override
	public TransactionsSummary getPreviousTransactions() {
		return summary;
	}

}
