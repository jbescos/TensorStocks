package com.jbescos.common;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class SymbolStats {

	private static final Logger LOGGER = Logger.getLogger(SymbolStats.class.getName());
	private final String symbol;
	// The higher the better
	private final double factor;
	private final double avg;
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
		this.avg = avg(values);
		this.newest = values.get(values.size() - 1);
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

	public String getSymbol() {
		return symbol;
	}

	public double getFactor() {
		return factor;
	}

	public double getAvg() {
		return avg;
	}
	
	public Action getAction() {
		return action;
	}

	public CsvRow getNewest() {
		return newest;
	}

	private Action evaluate(double price, double m) {
		Action action = Action.NOTHING;
		if (factor > CloudProperties.BOT_MIN_MAX_RELATION) {
			double buyCommision = (price * CloudProperties.BOT_BUY_COMISSION) + price;
			if (buyCommision < avg && m < 0) { // It is going up
				double percentileMin = ((avg - min.getPrice()) * CloudProperties.BOT_PERCENTILE_FACTOR) + min.getPrice();
				if (buyCommision < percentileMin) {
					action = Action.BUY;
				}
			} else if (price > avg && m > 0) { // It is going down
				double percentileMax = max.getPrice() - ((max.getPrice() - avg) * CloudProperties.BOT_PERCENTILE_FACTOR);
				if (price > percentileMax) {
					double minSell = CloudProperties.minSell(this.symbol);
					if (price < minSell) {
						LOGGER.info(Utils.format(price) + " " + this.symbol + " sell discarded because minimum selling price is set to " + Utils.format(minSell));
					} else if (price < minProfitableSellPrice) {
						LOGGER.info(Utils.format(price) + " " + this.symbol + " sell discarded because it has to be higher than " + Utils.format(minProfitableSellPrice) + " to be profitable");
					} else {
						action = Action.SELL;
					}
				}
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
	
	public static enum Action {
		BUY, SELL, NOTHING;
	}
}
