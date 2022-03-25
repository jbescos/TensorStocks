package com.jbescos.common;

import java.util.Date;

import com.jbescos.common.Broker.Action;

public class CsvTransactionRow implements IRow {

	private final Date date;
	private final String orderId;
	private final Action side;
	private final String symbol;
	private String usdt;
	private final String quantity;
	private final double usdtUnit;
	
	public CsvTransactionRow(Date date, String orderId, Action side, String symbol, String usdt, String quantity,
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

	public String getUsdt() {
		return usdt;
	}

	public String getQuantity() {
		return quantity;
	}

	public double getUsdtUnit() {
		return usdtUnit;
	}

    @Override
    public String toString() {
    	StringBuilder content = new StringBuilder().append("ORDER ID: ").append(orderId).append("\n").append(symbol).append(" BUY ").append(Utils.fromDate(Utils.FORMAT_SECOND, date))
    	    	.append("\nTotal USD (USD per unit): ").append(usdt).append("$ (").append(Utils.format(usdtUnit)).append("$)");
    	return content.toString();
    }

	public String toCsvLine() {
	    StringBuilder data = new StringBuilder();
        data.append(Utils.fromDate(Utils.FORMAT_SECOND, date)).append(",").append(orderId).append(",").append(side.name()).append(",").append(symbol).append(",").append(usdt).append(",").append(quantity).append(",").append(Utils.format(usdtUnit)).append("\r\n");
		return data.toString();
	}

	@Override
	public double getPrice() {
		return Double.parseDouble(usdt);
	}

	public void setUsdt(double usdt) {
		this.usdt = Double.toString(usdt);
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
