package com.jbescos.exchange;

import java.util.Date;

public class CsvAccountRow implements IRow {

	private final Date date;
	private final String symbol;
	private final double symbolValue;
	private final double usdt;
	
	public CsvAccountRow(Date date, String symbol, double symbolValue, double usdt) {
		this.date = date;
		this.symbol = symbol;
		this.symbolValue = symbolValue;
		this.usdt = usdt;
	}

	@Override
	public Date getDate() {
		return date;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getSymbolValue() {
		return symbolValue;
	}

	public double getUsdt() {
		return usdt;
	}

	@Override
	public double getPrice() {
		return getUsdt();
	}

	@Override
	public String getLabel() {
		return symbol;
	}

	@Override
	public Double getAvg() {
		return null;
	}

	@Override
	public Double getAvg2() {
		return null;
	}

}
