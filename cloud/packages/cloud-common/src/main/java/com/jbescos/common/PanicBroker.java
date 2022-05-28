package com.jbescos.common;

import java.util.logging.Logger;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.TransactionsSummary;

public class PanicBroker implements Broker {

	private static final Logger LOGGER = Logger.getLogger(PanicBroker.class.getName());
	private final String symbol;
	private final CsvRow newest;
	private final TransactionsSummary summary;
	private final Action action;

	public PanicBroker(String symbol, CsvRow newest, TransactionsSummary summary, Action action) {
		this.symbol = symbol;
		this.newest = newest;
		this.summary = summary;
		if (!summary.isHasTransactions() && (action == Action.SELL || action == Action.SELL_PANIC)) {
			this.action = Action.NOTHING;
		} else {
			this.action = action;
		}
	}

	@Override
	public Action getAction() {
		return action;
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
