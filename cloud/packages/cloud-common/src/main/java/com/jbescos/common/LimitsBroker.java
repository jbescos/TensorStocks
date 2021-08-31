package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

import com.jbescos.common.CloudProperties.FixedBuySell;

public class LimitsBroker implements Broker {
    
    private static final Logger LOGGER = Logger.getLogger(LimitsBroker.class.getName());
    private final String symbol;
    private final CsvRow newest;
    private Action action = Action.NOTHING;
    private CsvRow middle;
    private CsvRow oldest;

    public LimitsBroker(String symbol, List<CsvRow> values, FixedBuySell fixedBuySell) {
        this.symbol = symbol;
        this.newest = values.get(values.size() - 1);
        double price = newest.getPrice();
        if (values.size() > 1) {
            middle = values.get(values.size() - 2);
            if (values.size() > 2) {
                oldest = values.get(values.size() - 3);
            }
        }
        if (price >= fixedBuySell.getFixedSell()) {
            if (isMax()) {
                action = Action.SELL;
            } else {
                LOGGER.info(symbol + " discarded to buy because it is not a max");
            }
        } else if (price <= fixedBuySell.getFixedBuy()) {
            if (isMin()) {
                action = Action.BUY;
            } else {
                LOGGER.info(symbol + " discarded to buy because it is not a min");
            }
        } else {
            LOGGER.info(symbol + " discarded to buy because " + Utils.format(price) + " is between fixed limits " + Utils.format(fixedBuySell.getFixedBuy()) + " and " + Utils.format(fixedBuySell.getFixedSell()));
        }
    }

    private boolean isMin() {
        double first = newest.getPrice();
        if (middle != null) {
            double second = middle.getPrice();
            if (oldest != null) {
                double third = oldest.getPrice();
                return second <= first && second < third;
            }
        }
        return false;
    }
    
    private boolean isMax() {
        double first = newest.getPrice();
        if (middle != null) {
            double second = middle.getPrice();
            if (oldest != null) {
                double third = oldest.getPrice();
                return second >= first && second > third;
            }
        }
        return false;
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

}
