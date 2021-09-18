package com.jbescos.common;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class CautelousBroker implements Broker {

    private static final Logger LOGGER = Logger.getLogger(CautelousBroker.class.getName());
    private final String symbol;
    // The higher the better
    private final double factor;
    private final Double avg;
    private final CsvRow min;
    private final CsvRow max;
    private final CsvRow newest;
    private final Action action;
    private final double minProfitableSellPrice;
    private final boolean hasPreviousTransactions;
    private final Date lastPurchase;
    private final CloudProperties cloudProperties;

    public CautelousBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values, double minProfitableSellPrice, boolean hasPreviousTransactions, Date lastPurchase) {
    	this.cloudProperties = cloudProperties;
        this.symbol = symbol;
        this.min = Utils.getMinMax(values, true);
        this.max = Utils.getMinMax(values, false);
        this.factor = Utils.calculateFactor(min, max);
        this.newest = values.get(values.size() - 1);
        if (newest.getAvg() == null) {
            throw new IllegalArgumentException("Row does not contain AVG. It needs it to work: " + newest);
        } else {
            this.avg = newest.getAvg();
        }
        this.hasPreviousTransactions = hasPreviousTransactions;
        this.minProfitableSellPrice = minProfitableSellPrice;
        this.lastPurchase = lastPurchase;
        this.action = evaluate(newest.getPrice(), values);
    }
    
    public CautelousBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> values) {
        this(cloudProperties, symbol, values, 0, false, null);
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

    private Action evaluate(double price, List<CsvRow> values) {
        Action action = Action.NOTHING;
        double buyCommision = (price * cloudProperties.BOT_BUY_COMISSION) + price;
        if (buyCommision < avg) {
            double comparedFactor = cloudProperties.BOT_MIN_MAX_RELATION_BUY;
            if (!hasPreviousTransactions || (hasPreviousTransactions && buyCommision < minProfitableSellPrice)) {
                if (factor > comparedFactor) {
                    if (Utils.isMin(values)) { // It is going up
                        double percentileMin = ((avg - min.getPrice()) * cloudProperties.BOT_PERCENTILE_BUY_FACTOR) + min.getPrice();
                        if (buyCommision < percentileMin) {
                            action = Action.BUY;
                        } else {
                            LOGGER.info(() -> newest + " buy discarded because the price " + Utils.format(buyCommision) + " is higher than the acceptable value of " + Utils.format(percentileMin) + ". Min is " + min);
                        }
                    } else {
                        LOGGER.info(() -> newest + " buy discarded because it is not min.");
                    }
                } else {
                    LOGGER.info(() -> newest + " buy discarded to buy because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + ". Min " + min + " Max " + max);
                }
            } else {
                LOGGER.info(() -> newest + " buy discarded because current price is higher than what was bought before.");
            }
        } else if (hasPreviousTransactions) {
            if (Utils.isMax(values)) { // It is going down
                double minSell = cloudProperties.minSell(this.symbol);
                double sellCommision = (price * cloudProperties.BOT_SELL_COMISSION) + price;
                if (sellCommision < minSell) {
                    LOGGER.info(() -> newest + " sell discarded because minimum selling price is set to " + Utils.format(minSell) + ". Max is " + max);
                } else if (sellCommision < minProfitableSellPrice) {
                    LOGGER.info(() -> newest + " sell discarded because it has to be higher than " + Utils.format(minProfitableSellPrice) + " to be profitable.");
                } else {
                    double expectedBenefit = Utils.minProfitSellAfterDays(lastPurchase, newest.getDate(), cloudProperties.BOT_MIN_PROFIT_SELL, cloudProperties.BOT_PROFIT_DAYS_SUBSTRACTOR, cloudProperties.BOT_MAX_PROFIT_SELL);
                    double benefit = 1 - (minProfitableSellPrice / newest.getPrice());
                    if (benefit >= expectedBenefit) {
                    	LOGGER.info(() -> newest + " will try to sell. The expected benefit is " + Utils.format(expectedBenefit) + " and it is " + Utils.format(benefit));
                        action = Action.SELL;
                    } else {
                        LOGGER.info(() -> newest + " sell discarded because current benefit " + Utils.format(benefit) + " is lower than expected benefit " + Utils.format(expectedBenefit) + " calculated from last purchase " + Utils.fromDate(Utils.FORMAT_SECOND, lastPurchase));
                    }
                }
            } else {
                LOGGER.info(() -> newest + " sell discarded because it is not max.");
            }
        } else {
            LOGGER.info(() -> newest + " discarded because it is not good for buying and there is nothing to sell.");
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
        builder.append(", newest=").append(newest).append(", avg=").append(avg).append(", minProfitableSellPrice=").append(Utils.format(minProfitableSellPrice)).append(", action=").append(action.name()).append("\n");
        return builder.toString();
    }

}
