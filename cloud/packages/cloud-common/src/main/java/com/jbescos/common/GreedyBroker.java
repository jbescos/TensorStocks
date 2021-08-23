package com.jbescos.common;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class GreedyBroker implements Broker {
	
	private static final Logger LOGGER = Logger.getLogger(GreedyBroker.class.getName());
	private final String symbol;
	private final CsvRow newest;
	private final double minProfitableSellPrice;
	private final double factor;
	private final CsvRow min;
	private final CsvRow max;
	private Action action = Action.NOTHING;

	public GreedyBroker(String symbol, List<CsvRow> values, double minProfitableSellPrice, boolean hasPreviousTransactions, List<CsvTransactionRow> symbolTransactions) {
		this.symbol = symbol;
		this.newest = values.get(values.size() - 1);
		this.min = Utils.getMinMax(values, true);
		this.max = Utils.getMinMax(values, false);
		this.factor = Utils.calculateFactor(min, max);
		this.minProfitableSellPrice = minProfitableSellPrice;
		if (hasPreviousTransactions) {
			// SELL
			double acceptedPrice = minProfitableSellPrice + (minProfitableSellPrice * CloudProperties.BOT_GREEDY_MIN_PROFIT_SELL);
			if (newest.getPrice() > acceptedPrice) {
			    Date expirationHoldDate = Utils.getDateOfDaysBack(new Date(), CloudProperties.BOT_GREEDY_DAYS_TO_HOLD);
                CsvTransactionRow tx = symbolTransactions.get(0);
                acceptedPrice = minProfitableSellPrice + (minProfitableSellPrice * CloudProperties.BOT_GREEDY_IMMEDIATELY_SELL);
                if (tx.getDate().getTime() < expirationHoldDate.getTime() || newest.getPrice() > acceptedPrice) {
                    action = Action.SELL;
                } else {
                    LOGGER.info(symbol + " sell discarded because last transaction was " + Utils.fromDate(Utils.FORMAT_SECOND, tx.getDate()) + " is higher tha moving date " + Utils.fromDate(Utils.FORMAT_SECOND, expirationHoldDate));
                }
			} else {
				LOGGER.info(symbol + " sell discarded because price " + Utils.format(newest.getPrice()) + " is lower than min profitable " + Utils.format(acceptedPrice));
			}
		} else {
			// BUY
			if (!CloudProperties.BOT_NEVER_BUY_LIST_SYMBOLS.contains(symbol) && factor > CloudProperties.BOT_GREEDY_MIN_MAX_RELATION_BUY && inPercentileMin()) {
				action = Action.BUY;
			} else {
				LOGGER.info(symbol + " is not good for buying");
			}
		}
	}

	private boolean inPercentileMin() {
	    return Utils.inPercentile(CloudProperties.BOT_GREEDY_MIN_PERCENTILE_BUY, newest.getPrice(), min.getPrice(), max.getPrice()) == false;
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
		return CloudProperties.BOT_GREEDY_DEFAULT_FACTOR_SELL + factor;
	}
	
	   @Override
	    public String toString() {
	        StringBuilder builder = new StringBuilder("symbol=").append(symbol);
	        builder.append(", newest=").append(newest).append(", avg=").append(newest.getAvg()).append(", minProfitableSellPrice=").append(Utils.format(minProfitableSellPrice)).append(", action=").append(action.name()).append("\n");
	        return builder.toString();
	    }

}
