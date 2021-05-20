package com.jbescos.common;

public class Price {

	private String symbol;
	private double price;

	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	@Override
	public String toString() {
		return "Price [symbol=" + symbol + ", price=" + price + "]";
	}

}
