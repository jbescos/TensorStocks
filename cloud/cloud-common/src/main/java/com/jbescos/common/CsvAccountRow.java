package com.jbescos.common;

import java.util.Date;
import java.util.function.Supplier;

public class CsvAccountRow implements Supplier<Double> {

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
	public Double get() {
		return getUsdt();
	}

}
