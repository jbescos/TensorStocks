package com.jbescos.cloudbot;

import java.util.List;

import com.jbescos.common.CsvRow;
import com.jbescos.common.Utils;

public class SymbolStats {

	private static final double PERCENTILE_FACTOR = 0.05;
	private final String symbol;
	// The higher the better
	private final double factor;
	private final double avg;
	private final CsvRow min;
	private final CsvRow max;
	private final CsvRow newest;
	private final Action action;

	public SymbolStats(String symbol, List<CsvRow> values) {
		this.symbol = symbol;
		this.min = getMinMax(values, true);
		this.max = getMinMax(values, false);
		this.factor = calculateFactor(min, max);
		this.avg = avg(values);
		this.newest = values.get(values.size() - 1);
		this.action = evaluate(newest.getPrice());
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

	private Action evaluate(double price) {
		if (factor > Utils.MIN_MAX_FACTOR) {
			double buyCommision = (price * Utils.BUY_COMISSION) + price;
			if (buyCommision < avg) {
				double percentileMin = min.getPrice() + (min.getPrice() * PERCENTILE_FACTOR);
				if (buyCommision < percentileMin) {
					return Action.BUY;
				}
			} else if (price > avg) {
				double percentileMax = max.getPrice() - (max.getPrice() * PERCENTILE_FACTOR);
				if (price > percentileMax) {
					return Action.SELL;
				}
			}
		}
		return  Action.NOTHING;
	}
	
	private double avg(List<CsvRow> values) {
		double amount = 0;
		for (CsvRow row : values) {
			amount = amount + row.getPrice();
		}
		return amount / values.size();
	}
	
	private double calculateFactor(CsvRow min, CsvRow max) {
		return 1 - (min.getPrice() / max.getPrice());
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
		builder.append(", newest=").append(newest).append(", avg=").append(avg).append(", action=").append(action.name()).append("\n");
		return builder.toString();
	}
	
	public static enum Action {
		BUY, SELL, NOTHING;
	}
}
