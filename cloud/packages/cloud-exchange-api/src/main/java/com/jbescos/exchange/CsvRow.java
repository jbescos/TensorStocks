package com.jbescos.exchange;

import java.util.Date;

public class CsvRow implements IRow {

	private Date date;
	private String symbol;
	private double price;
	private Double avg;
	private Double avg2;
	private int fearGreedIndex;
	private Double fearGreedIndexAvg;
	private String token = "";
	
	public CsvRow(Date date, String symbol, double price, Double avg, Double avg2, int fearGreedIndex, Double fearGreedIndexAvg, String token) {
		this.date = date;
		this.symbol = symbol;
		this.price = price;
		this.avg = avg;
		this.avg2 = avg2;
		this.fearGreedIndex = fearGreedIndex;
		this.fearGreedIndexAvg = fearGreedIndexAvg;
		this.token = token;
	}

	public CsvRow(Date date, Price priceObj, Double avg, Double avg2, int fearGreedIndex, Double fearGreedIndexAvg) {
		this.date = date;
		this.symbol = priceObj.getSymbol();
		this.price = priceObj.getPrice();
		this.avg = avg;
		this.avg2 = avg2;
		this.fearGreedIndex = fearGreedIndex;
		this.fearGreedIndexAvg = fearGreedIndexAvg;
		this.token = priceObj.getToken();
	}
	
	public CsvRow(Date date, String symbol, double price) {
		this(date, symbol, price, null, null, 50, 50.0, "");
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public void setPrice(double price) {
		this.price = price;
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
		return "[" + symbol + ", " + Utils.fromDate(Utils.FORMAT_SECOND, date)+ ", " + Utils.format(price) + "]";
	}

	@Override
	public String getLabel() {
		return symbol;
	}

	@Override
	public Double getAvg2() {
		return avg2;
	}

	public void setAvg2(Double avg2) {
		this.avg2 = avg2;
	}
	
	public int getFearGreedIndex() {
		return fearGreedIndex;
	}

	public void setFearGreedIndex(int fearGreedIndex) {
		this.fearGreedIndex = fearGreedIndex;
	}

	public Double getFearGreedIndexAvg() {
		return fearGreedIndexAvg;
	}

	public void setFearGreedIndexAvg(Double fearGreedIndexAvg) {
		this.fearGreedIndexAvg = fearGreedIndexAvg;
	}

	public String toCsvLine() {
		StringBuilder builder = new StringBuilder();
		String dateStr = Utils.fromDate(Utils.FORMAT_SECOND, date);
		builder.append(dateStr).append(",").append(symbol).append(",").append(price).append(",").append(avg).append(",").append(avg2).append(",").append(fearGreedIndex).append(",").append(fearGreedIndexAvg).append(",").append(token)
		.append(Utils.NEW_LINE);
		return builder.toString();
	}
	
}
