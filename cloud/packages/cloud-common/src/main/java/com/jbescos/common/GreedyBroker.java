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
	private final CloudProperties cloudProperties;
	private final TransactionsSummary summary;

	public GreedyBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values, TransactionsSummary summary, List<CsvTransactionRow> symbolTransactions) {
		this.cloudProperties = cloudProperties;
		this.symbol = symbol;
		this.newest = values.get(values.size() - 1);
		this.min = Utils.getMinMax(values, true);
		this.max = Utils.getMinMax(values, false);
		this.factor = Utils.calculateFactor(min, max);
		this.minProfitableSellPrice = summary.getMinProfitable();
		this.summary = summary;
		if (summary.isHasTransactions()) {
			// SELL
			double acceptedPrice = minProfitableSellPrice + (minProfitableSellPrice * cloudProperties.BOT_GREEDY_MIN_PROFIT_SELL);
			if (newest.getPrice() > acceptedPrice) {
			    Date expirationHoldDate = Utils.getDateOfDaysBack(new Date(), cloudProperties.BOT_GREEDY_DAYS_TO_HOLD);
                CsvTransactionRow tx = symbolTransactions.get(0);
                acceptedPrice = minProfitableSellPrice + (minProfitableSellPrice * cloudProperties.BOT_GREEDY_IMMEDIATELY_SELL);
                if (tx.getDate().getTime() < expirationHoldDate.getTime() || newest.getPrice() > acceptedPrice) {
                    if (Utils.isMax(values) || ( 1 - (minProfitableSellPrice / newest.getPrice())) > cloudProperties.BOT_MAX_PROFIT_SELL) {
                        action = Action.SELL;
                    } else {
                        LOGGER.info(() -> newest + " discarded to sell because it is not a max");
                    }
                } else {
                    LOGGER.info(() -> newest + " sell discarded because last transaction was " + Utils.fromDate(Utils.FORMAT_SECOND, tx.getDate()) + " is higher than moving date " + Utils.fromDate(Utils.FORMAT_SECOND, expirationHoldDate));
                }
			} else {
				LOGGER.info(newest + " sell discarded because price is lower than min profitable " + Utils.format(acceptedPrice));
			}
		} else {
			// BUY
			if (factor > cloudProperties.BOT_GREEDY_MIN_MAX_RELATION_BUY && inPercentileMin()) {
			    if (Utils.isMin(values)) {
			        action = Action.BUY;
			    } else {
			        LOGGER.info(() -> newest + " discarded to buy because it is not a min");
			    }
			} else {
				LOGGER.info(() -> newest + " is not good for buying");
			}
		}
	}

	private boolean inPercentileMin() {
	    return Utils.inPercentile(cloudProperties.BOT_GREEDY_MIN_PERCENTILE_BUY, newest.getPrice(), min.getPrice(), max.getPrice()) == false;
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
		return cloudProperties.BOT_GREEDY_DEFAULT_FACTOR_SELL + factor;
	}

	@Override
	public TransactionsSummary getPreviousTransactions() {
		return summary;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("symbol=").append(symbol);
        builder.append(", newest=").append(newest).append(", avg=").append(newest.getAvg()).append(", minProfitableSellPrice=").append(Utils.format(minProfitableSellPrice)).append(", action=").append(action.name()).append("\n");
        return builder.toString();
    }

}
