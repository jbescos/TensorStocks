package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.TransactionsSummary;
import com.jbescos.exchange.Utils;

public class DCABroker implements Broker {
    
    private static final Logger LOGGER = Logger.getLogger(DCABroker.class.getName());
    private final double factor;
    private final String symbol;
    private final CsvRow newest;
    private final TransactionsSummary summary;
    private final List<CsvRow> values;
    private final CloudProperties cloudProperties;
    private Action action;

    public DCABroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values, TransactionsSummary summary) {
        this.cloudProperties = cloudProperties;
        this.values = values;
        CsvRow min = Utils.getMinMax(values, true);
        CsvRow max = Utils.getMinMax(values, false);
        this.newest = values.get(values.size() - 1);
        this.factor = Utils.calculateFactor(min, max);
        this.symbol = symbol;
        this.summary = summary;
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

    @Override
    public TransactionsSummary getPreviousTransactions() {
        return summary;
    }

    @Override
    public void evaluate(double avgBenefits) {
        double benefit = 1 - (summary.getMinProfitable() / newest.getPrice());
        if (isBuy(avgBenefits)) {
            action = Action.BUY;
        } else if (summary.isHasTransactions() && benefit >= cloudProperties.BOT_MAX_PROFIT_SELL) {
            action = Action.SELL;
        } else {
            action = Action.NOTHING;
        }
        
    }

    private boolean isBuy(double avgBenefits) {
        double currentPrice = newest.getPrice();
        if (currentPrice < newest.getAvg()) {
            double comparedFactor = cloudProperties.BOT_MIN_MAX_RELATION_BUY;
            if (factor > comparedFactor) {
                if (!summary.isHasTransactions()) {
                    return true;
                } else {
                    double lastPurchasedPrice = summary.getPreviousBuys().get(summary.getPreviousBuys().size() - 1).getUsdtUnit();
                    if (currentPrice < lastPurchasedPrice) {
                        double dropPriceRate = 1 - (currentPrice / lastPurchasedPrice);
                        if (dropPriceRate >= cloudProperties.BOT_DCA_RATIO_BUY) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public List<CsvRow> getValues() {
        return values;
    }

}
