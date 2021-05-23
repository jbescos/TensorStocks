package com.jbescos.common;

import java.util.Date;

import com.jbescos.common.SymbolStats.Action;

public class CsvTransactionRow {

	private final Date date;
	private final String orderId;
	private final Action side;
	private final String symbol;
	private final double usdt;
	private final double quantity;
	private final double usdtUnit;
	
	public CsvTransactionRow(Date date, String orderId, Action side, String symbol, double usdt, double quantity,
			double usdtUnit) {
		this.date = date;
		this.orderId = orderId;
		this.side = side;
		this.symbol = symbol;
		this.usdt = usdt;
		this.quantity = quantity;
		this.usdtUnit = usdtUnit;
	}

	public Date getDate() {
		return date;
	}

	public String getOrderId() {
		return orderId;
	}

	public Action getSide() {
		return side;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getUsdt() {
		return usdt;
	}

	public double getQuantity() {
		return quantity;
	}

	public double getUsdtUnit() {
		return usdtUnit;
	}

	@Override
	public String toString() {
		return "CsvTransactionRow [side=" + side + ", symbol=" + symbol + ", usdt=" + usdt + ", quantity=" + quantity
				+ ", usdtUnit=" + usdtUnit + "]";
	}
	
}
