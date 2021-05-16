package com.jbescos.common;

import java.util.Date;
import java.util.function.Supplier;

public class CsvRow implements Supplier<Double> {

	private final Date date;
	private final String symbol;
	private final double price;
	
	public CsvRow(Date date, String symbol, double price) {
		this.date = date;
		this.symbol = symbol;
		this.price = price;
	}

	public Date getDate() {
		return date;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getPrice() {
		return price;
	}

	@Override
	public String toString() {
		return "[" + Utils.fromDate(Utils.FORMAT_SECOND, date)+ ", " + price + "]";
	}

	@Override
	public Double get() {
		return getPrice();
	}
	
}
