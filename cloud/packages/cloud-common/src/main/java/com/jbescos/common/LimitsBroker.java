package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

import com.jbescos.common.CloudProperties.FixedBuySell;

public class LimitsBroker implements Broker {
    
    private static final Logger LOGGER = Logger.getLogger(LimitsBroker.class.getName());
    private final String symbol;
    private final CsvRow newest;
    private Action action = Action.NOTHING;

    public LimitsBroker(String symbol, List<CsvRow> values, FixedBuySell fixedBuySell) {
        this.symbol = symbol;
        this.newest = values.get(values.size() - 1);
        double price = newest.getPrice();
        if (price >= fixedBuySell.getFixedSell()) {
            if (Utils.isMax(values)) {
                action = Action.SELL;
            } else {
                LOGGER.info(() -> symbol + " discarded to buy because it is not a max");
            }
        } else if (price <= fixedBuySell.getFixedBuy()) {
            if (Utils.isMin(values)) {
                action = Action.BUY;
            } else {
                LOGGER.info(() -> symbol + " discarded to buy because it is not a min");
            }
        } else {
            LOGGER.info(() -> symbol + " discarded to buy because " + Utils.format(price) + " is between fixed limits " + Utils.format(fixedBuySell.getFixedBuy()) + " and " + Utils.format(fixedBuySell.getFixedSell()));
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
        return 0.3;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("symbol=").append(symbol);
        builder.append(", newest=").append(newest).append(", action=").append(action.name()).append("\n");
        return builder.toString();
    }
}
