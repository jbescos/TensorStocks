package com.jbescos.cloudbot;

import java.util.List;

import com.jbescos.common.CsvRow;

public class SymbolMinMax {

	private final String symbol;
	private final double factor;
	private final CsvRow min;
	private final CsvRow max;
	// The higher the better

	public SymbolMinMax(String symbol, List<CsvRow> values) {
		this.symbol = symbol;
		this.min = getMinMax(values, true);
		this.max = getMinMax(values, false);
		this.factor = calculateFactor(min, max);
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
		builder.append("\n");
		return builder.toString();
	}
}
