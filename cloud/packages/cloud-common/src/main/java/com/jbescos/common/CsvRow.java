package com.jbescos.common;

import java.util.Date;

public class CsvRow implements IRow {

	private final Date date;
	private final String symbol;
	private final double price;
	
	public CsvRow(Date date, String symbol, double price) {
		this.date = date;
		this.symbol = symbol;
		this.price = price;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String getSymbol() {
		return symbol;
	}

	@Override
	public double getPrice() {
		return price;
	}

	@Override
	public String toString() {
		return "[" + Utils.fromDate(Utils.FORMAT_SECOND, date)+ ", " + Utils.format(price) + "]";
	}
	
}
