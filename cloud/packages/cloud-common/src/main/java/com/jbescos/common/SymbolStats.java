package com.jbescos.common;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

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
	private final double minProfitableSellPrice;

	public SymbolStats(String symbol, List<CsvRow> values, List<CsvTransactionRow> previousTransactions) {
		this.symbol = symbol;
		this.min = getMinMax(values, true);
		this.max = getMinMax(values, false);
		this.factor = calculateFactor(min, max);
		this.newest = values.get(values.size() - 1);
		if (newest.getAvg() == null) {
			LOGGER.warning("The CSV does not contain the AVG!. It is being calculated from the last " + CloudProperties.BOT_DAYS_BACK_STATISTICS + " days. Row is " + newest);
			this.avg = avg(values);
		} else {
			this.avg = newest.getAvg();
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
		double buyCommision = (price * CloudProperties.BOT_BUY_COMISSION) + price;
		double sellCommision = (price * CloudProperties.BOT_SELL_COMISSION) + price;
		if (buyCommision < avg) {
			if (!CloudProperties.BOT_NEVER_BUY_LIST_SYMBOLS.contains(symbol)) {
				if (factor > CloudProperties.BOT_MIN_MAX_RELATION_BUY) {
					if (m < 0) { // It is going up
						double percentileMin = ((avg - min.getPrice()) * CloudProperties.BOT_PERCENTILE_FACTOR) + min.getPrice();
						if (buyCommision < percentileMin) {
							action = Action.BUY;
						} else {
							LOGGER.info(symbol + " discarded because the buy price " + Utils.format(buyCommision) + " is higher than the acceptable value of " + Utils.format(percentileMin));
						}
					} else {
						LOGGER.info(symbol + " buy discarded discarded because price is still going down");
					}
				} else {
					LOGGER.info(symbol + " discarded to buy because factor (1 - min/max) = " + factor + " is lower than the configured " + CloudProperties.BOT_MIN_MAX_RELATION_BUY 
							 + ". Min " + min + " Max " + max);
				}
			} else {
				LOGGER.info(symbol + " discarded to be bought because it is in the list of bot.never.buy");
			}
		} else if (sellCommision > avg) {
			double percentileMax = max.getPrice() - ((max.getPrice() - avg) * CloudProperties.BOT_PERCENTILE_FACTOR);
			if (factor > CloudProperties.BOT_MIN_MAX_RELATION_SELL) {
				if (sellCommision > percentileMax) {
					double minSell = CloudProperties.minSell(this.symbol);
					if (sellCommision < minSell) {
						LOGGER.info(Utils.format(sellCommision) + " " + this.symbol + " sell discarded because minimum selling price is set to " + Utils.format(minSell));
					} else if (sellCommision < minProfitableSellPrice) {
						LOGGER.info(Utils.format(sellCommision) + " " + this.symbol + " sell discarded because it has to be higher than " + Utils.format(minProfitableSellPrice) + " to be profitable");
					} else {
						action = Action.SELL;
					}
				} else {
					LOGGER.info(symbol + " discarded because the sell price " + Utils.format(sellCommision) + " is lower than the acceptable value of " + Utils.format(percentileMax));
				}
			} else {
				LOGGER.info(symbol + " discarded to sell because factor (1 - min/max) = " + factor + " is lower than the configured " + CloudProperties.BOT_MIN_MAX_RELATION_SELL 
						 + ". Min " + min + " Max " + max);
			}
		}
		return action;
	}
	
	private double avg(List<CsvRow> values) {
		Double prevousResult = null;
		for (CsvRow row : values) {
			prevousResult = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), prevousResult);
		}
		return prevousResult;
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
	
}
