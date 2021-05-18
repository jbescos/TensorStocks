package com.jbescos.common;

import java.util.Date;

public class CsvRow {

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
	
}
