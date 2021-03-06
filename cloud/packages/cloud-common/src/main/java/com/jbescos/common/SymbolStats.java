package com.jbescos.common;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.jbescos.common.CloudProperties.FixedBuySell;

public class SymbolStats implements BuySellAnalisys {

	private static final Logger LOGGER = Logger.getLogger(SymbolStats.class.getName());
	private final String symbol;
	// The higher the better
	private final double factor;
	private final Double avg;
	private final CsvRow min;
	private final CsvRow max;
	private final CsvRow newest;
	private final Action action;
	private final Mode mode;
	private final double minProfitableSellPrice;

	public SymbolStats(String symbol, List<CsvRow> values, List<CsvTransactionRow> previousTransactions) {
		this.symbol = symbol;
		this.min = getMinMax(values, true);
		this.max = getMinMax(values, false);
		this.factor = calculateFactor(min, max);
		this.newest = values.get(values.size() - 1);
		if (newest.getAvg() == null) {
			throw new IllegalArgumentException("Row does not contain AVG. It needs it to work: " + newest);
		} else {
			this.avg = newest.getAvg();
		}
		if (newest.getPrice() > newest.getAvg2()) {
			mode = Mode.BULLISH;
		} else {
			mode = Mode.BEARISH;
		}
		double m = 0;
		if (values.size() > 1) {
			CsvRow secondNewest = values.get(values.size() - 2);
			m = secondNewest.getPrice() - newest.getPrice();
		}
		this.minProfitableSellPrice = Utils.minSellProfitable(previousTransactions);
		this.action = evaluate(newest.getPrice(), m);
	}
	
	public SymbolStats(String symbol, List<CsvRow> values) {
		this(symbol, values, Collections.emptyList());
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

	private Action evaluate(double price, double m) {
		Action action = Action.NOTHING;
		FixedBuySell fixedBuySell = CloudProperties.FIXED_BUY_SELL.get(symbol);
		if (fixedBuySell != null) {
			if (price >= fixedBuySell.getFixedSell()) {
				action = Action.SELL;
			} else if (price <= fixedBuySell.getFixedBuy()) {
				action = Action.BUY;
			} else {
				LOGGER.info(symbol + " discarded because " + Utils.format(price) + " is between fixed limits " + Utils.format(fixedBuySell.getFixedBuy()) + " and " + Utils.format(fixedBuySell.getFixedSell()));
			}
		} else {
			double buyCommision = (price * CloudProperties.BOT_BUY_COMISSION) + price;
			double sellCommision = (price * CloudProperties.BOT_SELL_COMISSION) + price;
			if (buyCommision < avg) {
				if (!CloudProperties.BOT_NEVER_BUY_LIST_SYMBOLS.contains(symbol)) {
					double comparedFactor = getComparedFactor(Action.BUY);
					if (factor > comparedFactor) {
						if (m < 0) { // It is going up
							double percentileMin = ((avg - min.getPrice()) * CloudProperties.BOT_PERCENTILE_BUY_FACTOR) + min.getPrice();
							if (buyCommision < percentileMin) {
								action = Action.BUY;
							} else {
								LOGGER.info(symbol + " discarded because the buy price " + Utils.format(buyCommision) + " is higher than the acceptable value of " + Utils.format(percentileMin) + ". Min is " + min);
							}
						} else {
							LOGGER.info(symbol + " buy discarded because price is still going down");
						}
					} else {
						LOGGER.info(symbol + " discarded to buy because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + " for " + mode
								 + ". Min " + min + " Max " + max);
					}
				} else {
					LOGGER.info(symbol + " discarded to be bought because it is in the list of bot.never.buy");
				}
			} else if (sellCommision > avg) {
				double percentileMax = max.getPrice() - ((max.getPrice() - avg) * CloudProperties.BOT_PERCENTILE_SELL_FACTOR);
				double comparedFactor = getComparedFactor(Action.SELL);
				if (factor > comparedFactor) {
				    if (m > 0) { // It is going up
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
				        LOGGER.info(symbol + " sell discarded because price is still going up");
				    }
				} else {
					LOGGER.info(symbol + " discarded to sell because factor (1 - min/max) = " + factor + " is lower than the configured " + comparedFactor + " for " + mode
							 + ". Min " + min + " Max " + max);
				}
			}
		}
		return action;
	}
	
	private double getComparedFactor(Action action) {
		if (action == Action.SELL) {
			if (mode == Mode.BULLISH) {
				return CloudProperties.BOT_MIN_MAX_RELATION_SELL_BULLISH;
			} else {
				return CloudProperties.BOT_MIN_MAX_RELATION_SELL_BEARISH;
			}
		} else if (action == Action.BUY) {
			if (mode == Mode.BULLISH) {
				return CloudProperties.BOT_MIN_MAX_RELATION_BUY_BULLISH;
			} else {
				return CloudProperties.BOT_MIN_MAX_RELATION_BUY_BEARISH;
			}
		}
		return 0;
	}
	
	private double calculateFactor(CsvRow min, CsvRow max) {
		double factor =  1 - (min.getPrice() / max.getPrice());
//		LOGGER.info("MIN is " + min.getPrice() + " MAX is " + max.getPrice() + ". Factor " + factor);
		return factor;
	}

	private CsvRow getMinMax(List<CsvRow> values, boolean min) {
		CsvRow result = values.get(0);
		for (CsvRow row : values) {
			if ((min && row.getPrice() < result.getPrice()) || (!min && row.getPrice() > result.getPrice())) {
				result = row;
			}
		}
		return result;
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
	
	private static enum Mode {
		BULLISH, BEARISH;
	}

	private static enum Style {
		PESSIMISTIC, OPTIMISTIC;
	}
}
