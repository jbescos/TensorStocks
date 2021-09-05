package com.jbescos.common;

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

    public CautelousBroker(String symbol, List<CsvRow> values, double minProfitableSellPrice, boolean hasPreviousTransactions) {
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
        this.action = evaluate(newest.getPrice(), values);
    }
    
    public CautelousBroker(String symbol, List<CsvRow> values) {
        this(symbol, values, 0, false);
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
        if (hasPreviousTransactions && ( 1 - (minProfitableSellPrice / price)) > CloudProperties.BOT_SELL_BENEFIT_COMPARED_TRANSACTIONS) {
            action = Action.SELL;
            LOGGER.info(symbol + " is going to be sold because current price " + Utils.format(price) + " is more than " + CloudProperties.BOT_SELL_BENEFIT_COMPARED_TRANSACTIONS + " times higher than minProfitableSellPrice " + Utils.format(minProfitableSellPrice));
        } else {
            double sellCommision = (price * CloudProperties.BOT_SELL_COMISSION) + price;
            if (buyCommision < avg) {
                double comparedFactor = CloudProperties.BOT_MIN_MAX_RELATION_BUY;
                if (!hasPreviousTransactions || (hasPreviousTransactions && buyCommision < minProfitableSellPrice)) {
                	if (factor > comparedFactor) {
                        if (Utils.isMin(values)) { // It is going up
                            double percentileMin = ((avg - min.getPrice()) * CloudProperties.BOT_PERCENTILE_BUY_FACTOR) + min.getPrice();
                            if (buyCommision < percentileMin) {
                                action = Action.BUY;
                            } else {
                                LOGGER.info(symbol + " discarded because the buy price " + Utils.format(buyCommision) + " is higher than the acceptable value of " + Utils.format(percentileMin) + ". Min is " + min);
                            }
                        } else {
                            LOGGER.info(symbol + " buy discarded because it is not min");
                        }
                    } else {
                        LOGGER.info(symbol + " discarded to buy because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + ". Min " + min + " Max " + max);
                    }
                } else {
                	LOGGER.info(symbol + " buy discarded because current price is higher than what was bought before");
                }
            } else if (sellCommision > avg) {
                double percentileMax = max.getPrice() - ((max.getPrice() - avg) * CloudProperties.BOT_PERCENTILE_SELL_FACTOR);
                double comparedFactor = CloudProperties.BOT_MIN_MAX_RELATION_SELL;
                if (factor > comparedFactor) {
                    if (Utils.isMax(values)) { // It is going down
                        if (sellCommision > percentileMax) {
                            double minSell = CloudProperties.minSell(this.symbol);
                            if (sellCommision < minSell) {
                                LOGGER.info(Utils.format(sellCommision) + " " + this.symbol + " sell discarded because minimum selling price is set to " + Utils.format(minSell) + ". Max is " + max);
                            } else if (sellCommision < minProfitableSellPrice) {
                                LOGGER.info(Utils.format(sellCommision) + " " + this.symbol + " sell discarded because it has to be higher than " + Utils.format(minProfitableSellPrice) + " to be profitable");
                            } else {
                                action = Action.SELL;
                            }
                        } else {
                            LOGGER.info(symbol + " discarded because the sell price " + Utils.format(sellCommision) + " is lower than the acceptable value of " + Utils.format(percentileMax));
                        }
                    } else {
                        LOGGER.info(symbol + " sell discarded because it is not max");
                    }
                } else {
                    LOGGER.info(symbol + " discarded to sell because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + ". Min " + min + " Max " + max);
                }
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
        builder.append(", newest=").append(newest).append(", avg=").append(avg).append(", minProfitableSellPrice=").append(Utils.format(minProfitableSellPrice)).append(", action=").append(action.name()).append("\n");
        return builder.toString();
    }

}
