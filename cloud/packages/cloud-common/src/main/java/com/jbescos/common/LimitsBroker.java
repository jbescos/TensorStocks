package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

import com.jbescos.common.CloudProperties.FixedBuySell;

public class LimitsBroker implements Broker {
    
    private static final Logger LOGGER = Logger.getLogger(LimitsBroker.class.getName());
    private final CloudProperties cloudProperties;
    private final String symbol;
    private final CsvRow newest;
    private final CsvRow min;
    private final CsvRow max;
    private final double minMaxFactor;
    private final TransactionsSummary summary;
    private Action action = Action.NOTHING;

    public LimitsBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values, FixedBuySell fixedBuySell, TransactionsSummary summary) {
    	this.cloudProperties = cloudProperties;
        this.symbol = symbol;
        this.newest = values.get(values.size() - 1);
        this.min = Utils.getMinMax(values, true);
        this.max = Utils.getMinMax(values, false);
        this.minMaxFactor = Utils.calculateFactor(min, max);
        this.summary = summary;
        double price = newest.getPrice();
        if (price >= fixedBuySell.getFixedSell()) {
            if (Utils.isMax(values)) {
                action = Action.SELL;
            } else {
//                LOGGER.info(() -> newest + " discarded to sell because it is not a max");
            }
        } else if (price <= fixedBuySell.getFixedBuy() && price < summary.getLowestPurchase()) {
            if (Utils.isMin(values)) {
                action = Action.BUY;
            } else {
//                LOGGER.info(() -> newest + " discarded to buy because it is not a min");
            }
        } else {
//            LOGGER.info(() -> newest + " discarded to buy because price is between fixed limits " + Utils.format(fixedBuySell.getFixedBuy()) + " and " + Utils.format(fixedBuySell.getFixedSell()));
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
        return Utils.factorMultiplier(minMaxFactor, cloudProperties.BOT_LIMITS_FACTOR_MULTIPLIER);
    }

	@Override
	public TransactionsSummary getPreviousTransactions() {
		return summary;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("symbol=").append(symbol);
        builder.append(", newest=").append(newest).append(", action=").append(action.name()).append("\n");
        return builder.toString();
    }
}
