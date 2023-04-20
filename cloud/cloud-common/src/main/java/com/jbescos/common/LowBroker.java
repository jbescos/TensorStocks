package com.jbescos.common;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.TransactionsSummary;
import com.jbescos.exchange.Utils;

public class LowBroker implements Broker {

    private static final Logger LOGGER = Logger.getLogger(LowBroker.class.getName());
    private final String symbol;
    // The higher the better
    private final double factor;
    private final Double avg;
    private final CsvRow min;
    private final CsvRow max;
    private final CsvRow newest;
    private Action action;
    private final double minProfitableSellPrice;
    private final boolean hasPreviousTransactions;
    private final Date lastPurchase;
    private final TransactionsSummary summary;
    private final CloudProperties cloudProperties;
    private final boolean percentileMin;
    private final List<CsvRow> values;

    public LowBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values,
            TransactionsSummary summary) {
        this.values = values;
        this.cloudProperties = cloudProperties;
        this.symbol = symbol;
        this.min = Utils.getMinMax(values, true);
        this.max = Utils.getMinMax(values, false);
        this.newest = values.get(values.size() - 1);
        this.factor = Utils.calculateFactor(min, max);
        if (newest.getAvg() == null) {
            throw new IllegalArgumentException("Row does not contain AVG. It needs it to work: " + newest);
        } else {
            this.avg = newest.getAvg();
        }
        this.summary = summary;
        this.hasPreviousTransactions = summary.isHasTransactions();
        this.minProfitableSellPrice = summary.getMinProfitable();
        this.lastPurchase = summary.getLastPurchase();
        double percentileMin = ((avg - min.getPrice()) * cloudProperties.BOT_PERCENTILE_BUY_FACTOR) + min.getPrice();
        this.percentileMin = newest.getPrice() < percentileMin;
    }

    public LowBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values) {
        this(cloudProperties, symbol, values, new TransactionsSummary(false, 0, Double.MAX_VALUE, null,
                Collections.emptyList(), Collections.emptyList()));
    }

    public CsvRow getMin() {
        return min;
    }

    public CsvRow getMax() {
        return max;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public double getFactor() {
        return factor;
    }

    public double getAvg() {
        return avg;
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
    public TransactionsSummary getPreviousTransactions() {
        return summary;
    }

    public boolean inPercentileMin() {
        return percentileMin;
    }

    private Action evaluate(double price, double summaryAvg) {
        Action action = Action.NOTHING;
        double benefit = 1 - (minProfitableSellPrice / newest.getPrice());
        double maxProfitSell = Utils.isFearMode(newest.getFearGreedIndex())
                ? cloudProperties.BOT_MIN_PROFIT_SELL
                : cloudProperties.BOT_MAX_PROFIT_SELL;
        if (hasPreviousTransactions && Utils.isMax(values)
                && benefit >= Utils.minProfitSellAfterDays(lastPurchase, newest.getDate(), maxProfitSell,
                        cloudProperties.BOT_PROFIT_DAYS_SUBSTRACTOR, maxProfitSell,
                        cloudProperties.BOT_LOWEST_ALLOWED_PROFIT_SELL)) {
            action = Action.SELL;
        } else {
            if (price < avg) {
                double comparedFactor = Utils.factorFearGreedAdjusted(cloudProperties.BOT_MIN_MAX_RELATION_BUY, newest, summaryAvg);
                if (!hasPreviousTransactions || (hasPreviousTransactions
                        && Utils.isLowerPurchase(price, summary.getLowestPurchase(), Utils.LOWER_PURCHASE_REDUCER))) {
                    if (factor > comparedFactor) {
                        if (Utils.isMin(values)) { // It is going up
                            if (percentileMin) {
                                action = Action.BUY;
                            } else {
//                                LOGGER.info(() -> newest + " buy discarded because the price is higher than the acceptable value of " + Utils.format(percentileMin) + ". Min is " + min);
                            }
                        } else {
//                            LOGGER.info(() -> newest + " buy discarded because it is not min.");
                        }
                    } else {
//                        LOGGER.info(() -> newest + " buy discarded to buy because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + ". Min " + min + " Max " + max);
                    }
                } else {
//                    LOGGER.info(() -> newest + " buy discarded because current price is higher than lowest purchase " + Utils.format(summary.getLowestPurchase()));
                }
            } else if (hasPreviousTransactions) {
                if (Utils.isMax(values)) { // It is going down
                    if (price < minProfitableSellPrice) {
//                        LOGGER.info(() -> newest + " sell discarded because it has to be higher than " + Utils.format(minProfitableSellPrice) + " to be profitable.");
                    } else {
                        double expectedBenefit = Utils.minProfitSellAfterDays(lastPurchase, newest.getDate(),
                                cloudProperties.BOT_MIN_PROFIT_SELL, cloudProperties.BOT_PROFIT_DAYS_SUBSTRACTOR,
                                maxProfitSell, cloudProperties.BOT_LOWEST_ALLOWED_PROFIT_SELL);
                        if (benefit >= expectedBenefit) {
                            LOGGER.info(() -> newest + " will try to sell. The expected benefit is "
                                    + Utils.format(expectedBenefit) + " and it is " + Utils.format(benefit));
                            action = Action.SELL;
                        } else {
//                            LOGGER.info(() -> newest + " sell discarded because current benefit " + Utils.format(benefit) + " is lower than expected benefit " + Utils.format(expectedBenefit) + " calculated from last purchase " + Utils.fromDate(Utils.FORMAT_SECOND, lastPurchase));
                        }
                    }
                } else {
//                    LOGGER.info(() -> newest + " sell discarded because it is not max.");
                }
            } else {
                // LOGGER.info(() -> newest + " discarded because it is not good for buying and
                // there is nothing to sell.");
            }
        }
        return action;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("symbol=").append(symbol).append(", factor=").append(factor);
        if (min.getDate().getTime() < max.getDate().getTime()) {
            builder.append(", min=").append(min).append(", max=").append(max);
        } else {
            builder.append(", max=").append(max).append(", min=").append(min);
        }
        builder.append(", newest=").append(newest).append(", avg=").append(avg).append(", minProfitableSellPrice=")
                .append(Utils.format(minProfitableSellPrice)).append(", action=").append(action.name()).append("\n");
        return builder.toString();
    }

    @Override
    public void evaluate(double summaryAvg) {
        this.action = evaluate(newest.getPrice(), summaryAvg);
    }

    @Override
    public List<CsvRow> getValues() {
        return values;
    }

}
