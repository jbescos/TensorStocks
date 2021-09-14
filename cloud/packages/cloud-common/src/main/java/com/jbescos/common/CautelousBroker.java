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

    public CautelousBroker(String symbol, List<CsvRow> values, double minProfitableSellPrice, boolean hasPreviousTransactions, Date lastPurchase) {
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
    
    public CautelousBroker(String symbol, List<CsvRow> values) {
        this(symbol, values, 0, false, null);
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
        double buyCommision = (price * CloudProperties.BOT_BUY_COMISSION) + price;
        if (buyCommision < avg) {
            double comparedFactor = CloudProperties.BOT_MIN_MAX_RELATION_BUY;
            if (!hasPreviousTransactions || (hasPreviousTransactions && buyCommision < minProfitableSellPrice)) {
                if (factor > comparedFactor) {
                    if (Utils.isMin(values)) { // It is going up
                        double percentileMin = ((avg - min.getPrice()) * CloudProperties.BOT_PERCENTILE_BUY_FACTOR) + min.getPrice();
                        if (buyCommision < percentileMin) {
                            action = Action.BUY;
                        } else {
                            LOGGER.info(() -> symbol + " discarded because the buy price " + Utils.format(buyCommision) + " is higher than the acceptable value of " + Utils.format(percentileMin) + ". Min is " + min + ". Current price is " + newest);
                        }
                    } else {
                        LOGGER.info(() -> symbol + " buy discarded because it is not min. Current price is " + newest);
                    }
                } else {
                    LOGGER.info(() -> symbol + " discarded to buy because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + ". Min " + min + " Max " + max + ". Current price is " + newest);
                }
            } else {
                LOGGER.info(() -> symbol + " buy discarded because current price is higher than what was bought before. Current price is " + newest);
            }
        } else if (hasPreviousTransactions) {
            if (Utils.isMax(values)) { // It is going down
                double minSell = CloudProperties.minSell(this.symbol);
                double sellCommision = (price * CloudProperties.BOT_SELL_COMISSION) + price;
                if (sellCommision < minSell) {
                    LOGGER.info(() -> Utils.format(sellCommision) + " " + this.symbol + " sell discarded because minimum selling price is set to " + Utils.format(minSell) + ". Max is " + max + ". Current price is " + newest);
                } else if (sellCommision < minProfitableSellPrice) {
                    LOGGER.info(() -> Utils.format(sellCommision) + " " + this.symbol + " sell discarded because it has to be higher than " + Utils.format(minProfitableSellPrice) + " to be profitable. Current price is " + newest);
                } else {
                    double expectedBenefit = Utils.minProfitSellAfterDays(lastPurchase, newest.getDate(), CloudProperties.BOT_MIN_PROFIT_SELL, CloudProperties.BOT_PROFIT_DAYS_SUBSTRACTOR, CloudProperties.BOT_SELL_BENEFIT_COMPARED_TRANSACTIONS);
                    double benefit = 1 - (minProfitableSellPrice / newest.getPrice());
                    if (benefit >= expectedBenefit) {
                    	LOGGER.info(() -> symbol + " will try to sell. The expected benefit is " + Utils.format(expectedBenefit) + " and it is " + Utils.format(benefit) + ". Current price is " + newest);
                        action = Action.SELL;
                    } else {
                        LOGGER.info(() -> symbol + " sell discarded because current benefit " + Utils.format(benefit) + " is lower than expected benefit " + Utils.format(expectedBenefit) + " calculated from last purchase " + Utils.fromDate(Utils.FORMAT_SECOND, lastPurchase) + ". Current price is " + newest);
                    }
                }
            } else {
                LOGGER.info(() -> symbol + " sell discarded because it is not max. Current price is " + newest);
            }
        } else {
            LOGGER.info(() -> symbol + " discarded because it is not good for buying and there is nothing to sell. Current price is " + newest);
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
