package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.TransactionsSummary;
import com.jbescos.exchange.Utils;

public class LimitsBroker implements Broker {
    
    private static final Logger LOGGER = Logger.getLogger(LimitsBroker.class.getName());
    private final CloudProperties cloudProperties;
    private final String symbol;
    private final CsvRow newest;
    private final CsvRow min;
    private final CsvRow max;
    private final double minMaxFactor;
    private final TransactionsSummary summary;
    private final Action action;

    public LimitsBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values, double fixedBuy, TransactionsSummary summary) {
    	this.cloudProperties = cloudProperties;
        this.symbol = symbol;
        this.newest = values.get(values.size() - 1);
        this.min = Utils.getMinMax(values, true);
        this.max = Utils.getMinMax(values, false);
        this.minMaxFactor = Utils.calculateFactor(min, max);
        this.summary = summary;
        double price = newest.getPrice();
        double benefit = 1 - (summary.getMinProfitable() / newest.getPrice());
        if (summary.isHasTransactions() && Utils.isMax(values) && isSell(benefit)) {
            action = Action.SELL;
        } else if (price <= fixedBuy && Utils.isMin(values) && (!summary.isHasTransactions() || (summary.isHasTransactions() && Utils.isLowerPurchase(price, summary.getLowestPurchase(), Utils.LOWER_LIMITS_PURCHASE_REDUCER)))) {
            action = Action.BUY;
        } else {
        	action = Action.NOTHING;
        }
    }
    
    private boolean isSell(double benefit) {
        Double fixedSell =  cloudProperties.FIXED_SELL.get(symbol);
        if (fixedSell != null) {
            return newest.getPrice() >= fixedSell;
        } else {
            return benefit >= cloudProperties.BOT_LIMITS_FACTOR_PROFIT_SELL;
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
