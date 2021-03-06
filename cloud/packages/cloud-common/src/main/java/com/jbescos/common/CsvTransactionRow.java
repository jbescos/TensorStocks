package com.jbescos.common;

import java.util.Date;

import com.jbescos.common.BuySellAnalisys.Action;

public class CsvTransactionRow implements IRow {

	private final Date date;
	private final String orderId;
	private final Action side;
	private final String symbol;
	private double usdt;
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

	@Override
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
		return " TX [date=" + Utils.fromDate(Utils.FORMAT_SECOND, date) + ", side=" + side + ", symbol=" + symbol + ", usdt=" + Utils.format(usdt)
				+ ", quantity=" + quantity + ", usdtUnit=" + Utils.format(usdtUnit) + "]\n";
	}

	@Override
	public double getPrice() {
		return usdt;
	}

	public void setUsdt(double usdt) {
		this.usdt = usdt;
	}

	@Override
	public String getLabel() {
		return side.name() + "-" + symbol;
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
