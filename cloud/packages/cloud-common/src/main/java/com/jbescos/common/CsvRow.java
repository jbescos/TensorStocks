package com.jbescos.common;

import java.util.Date;

public class CsvRow implements IRow {

	private final Date date;
	private final String symbol;
	private final double price;
	private Double avg;
	
	public CsvRow(Date date, String symbol, double price, Double avg) {
		this.date = date;
		this.symbol = symbol;
		this.price = price;
		this.avg = avg;
	}
	
	public CsvRow(Date date, String symbol, double price) {
		this(date, symbol, price, null);
	}

	@Override
	public Date getDate() {
		return date;
	}

	public String getSymbol() {
		return symbol;
	}

	@Override
	public Double getAvg() {
		return avg;
	}

	public void setAvg(Double avg) {
		this.avg = avg;
	}

	@Override
	public double getPrice() {
		return price;
	}

	@Override
	public String toString() {
		return "[" + Utils.fromDate(Utils.FORMAT_SECOND, date)+ ", " + Utils.format(price) + "]";
	}

	@Override
	public String getLabel() {
		return symbol;
	}
	
}
