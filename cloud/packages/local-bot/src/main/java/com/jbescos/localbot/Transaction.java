package com.jbescos.localbot;

public class Transaction {
	// DATE,ORDER_ID,SIDE,SYMBOL,USDT,QUANTITY,USDT_UNIT
	private final String date;
	private final String orderId;
	private final String side;
	private final String symbol;
	private final String usdt;
	private final String quantity;
	private final String usdtUnit;
	
	public Transaction(String date, String orderId, String side, String symbol, String usdt, String quantity,
			String usdtUnit) {
		this.date = date;
		this.orderId = orderId;
		this.side = side;
		this.symbol = symbol;
		this.usdt = usdt;
		this.quantity = quantity;
		this.usdtUnit = usdtUnit;
	}
	public String getDate() {
		return date;
	}
	public String getOrderId() {
		return orderId;
	}
	public String getSide() {
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
	public String getUsdtUnit() {
		return usdtUnit;
	}
	@Override
	public String toString() {
		return "Transaction [date=" + date + ", orderId=" + orderId + ", side=" + side + ", symbol=" + symbol
				+ ", usdt=" + usdt + ", quantity=" + quantity + ", usdtUnit=" + usdtUnit + "]";
	}
}
