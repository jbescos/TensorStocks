package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

public class GreedyBroker implements Broker {
	
	private static final Logger LOGGER = Logger.getLogger(GreedyBroker.class.getName());
	private final String symbol;
	private final CsvRow newest;
	private double factor = 0;
	private Action action = Action.NOTHING;
	private CsvRow secondNewest;
	private static final double MIN_FACTOR_TO_BUY = 0.01;
	private static final double MIN_PROFIT_TO_SELL = 0.05;

	public GreedyBroker(String symbol, List<CsvRow> values, double minProfitableSellPrice, boolean hasPreviousTransactions) {
		this.symbol = symbol;
		this.newest = values.get(values.size() - 1);
		if (values.size() > 1) {
			this.secondNewest = values.get(values.size() - 2);
			if (newest.getPrice() > secondNewest.getPrice()) {
				this.factor = Utils.calculateFactor(secondNewest, newest);
				// SELL
				minProfitableSellPrice = minProfitableSellPrice + (minProfitableSellPrice * MIN_PROFIT_TO_SELL);
				if (newest.getPrice() > minProfitableSellPrice) {
					action = Action.SELL;
				} else {
					LOGGER.info(symbol + " sell discarded because price is lower than min profitable " + Utils.format(minProfitableSellPrice));
				}
			} else {
				this.factor = Utils.calculateFactor(newest, secondNewest);
				// BUY
				if (factor > MIN_FACTOR_TO_BUY) {
					action = Action.BUY;
				} else {
					LOGGER.info(symbol + " buy discarded because min profit to sell is set to " + MIN_FACTOR_TO_BUY + " and the current one is " + factor);
				}
			}
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
		return factor;
	}

}
