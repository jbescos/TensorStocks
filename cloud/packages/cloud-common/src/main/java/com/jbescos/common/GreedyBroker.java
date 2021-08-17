package com.jbescos.common;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class GreedyBroker implements Broker {
	
	private static final Logger LOGGER = Logger.getLogger(GreedyBroker.class.getName());
	private final String symbol;
	private final CsvRow newest;
	private double factor = 0;
	private Action action = Action.NOTHING;
	private CsvRow secondNewest;
	private static final double MIN_FACTOR_TO_BUY = CloudProperties.BOT_GREEDY_MIN_FACTOR_BUY;
	private static final double MIN_PROFIT_TO_SELL = CloudProperties.BOT_GREEDY_MIN_PROFIT_SELL;

	public GreedyBroker(String symbol, List<CsvRow> values, double minProfitableSellPrice, boolean hasPreviousTransactions, List<CsvTransactionRow> symbolTransactions) {
		this.symbol = symbol;
		this.newest = values.get(values.size() - 1);
		if (values.size() > 1) {
			this.secondNewest = values.get(values.size() - 2);
			if (hasPreviousTransactions) {
				this.factor = Utils.calculateFactor(secondNewest, newest);
				// SELL
				minProfitableSellPrice = minProfitableSellPrice + (minProfitableSellPrice * MIN_PROFIT_TO_SELL);
				if (newest.getPrice() > minProfitableSellPrice) {
				    Date expirationHoldDate = Utils.getDateOfDaysBack(new Date(), CloudProperties.BOT_GREEDY_DAYS_TO_HOLD);
	                CsvTransactionRow tx = symbolTransactions.get(0);
	                if (tx.getDate().getTime() < expirationHoldDate.getTime()) {
	                    action = Action.SELL;
	                } else {
	                    LOGGER.info(symbol + " sell discarded because last transaction was " + Utils.fromDate(Utils.FORMAT_SECOND, tx.getDate()) + " and it will hold till " + Utils.fromDate(Utils.FORMAT_SECOND, expirationHoldDate));
	                }
				} else {
					LOGGER.info(symbol + " sell discarded because price is lower than min profitable " + Utils.format(minProfitableSellPrice));
				}
			} else {
				this.factor = Utils.calculateFactor(newest, secondNewest);
				// BUY
				if (!CloudProperties.BOT_NEVER_BUY_LIST_SYMBOLS.contains(symbol) && factor > MIN_FACTOR_TO_BUY) {
					action = Action.BUY;
				} else {
					LOGGER.info(symbol + " buy discarded because min profit to sell is set to " + MIN_FACTOR_TO_BUY + " and the current one is " + factor + " or is in the bot.never.buy");
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
		return CloudProperties.BOT_GREEDY_DEFAULT_FACTOR_SELL;
	}

}
