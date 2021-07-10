package com.jbescos.common;

import java.util.Date;

public class CsvRow implements IRow {

	private Date date;
	private String symbol;
	private double price;
	private Double avg;
	private Double avg2;
	private Kline kline;
	
	public CsvRow(Date date, String symbol, double price, Double avg, Double avg2) {
		this.date = date;
		this.symbol = symbol;
		this.price = price;
		this.avg = avg;
		this.avg2 = avg2;
	}
	
	public CsvRow(Date date, String symbol, double price) {
		this(date, symbol, price, null, null);
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

	public Kline getKline() {
		return kline;
	}

	public void setKline(Kline kline) {
		this.kline = kline;
	}
	
	public String toCsvLine() {
		StringBuilder builder = new StringBuilder();
		String dateStr = Utils.fromDate(Utils.FORMAT_SECOND, date);
		String volumeBuy = kline == null ? Utils.EMPTY_STR : kline.getTakerBuyBaseAssetVolume();
		String volume = kline == null ? Utils.EMPTY_STR : kline.getVolume();
		String open = kline == null ? Utils.EMPTY_STR : kline.getOpen();
		String close = kline == null ? Utils.EMPTY_STR : kline.getClose();
		builder.append(dateStr).append(",").append(getSymbol()).append(",").append(getPrice()).append(",").append(getAvg()).append(",").append(getAvg2())
		.append(",").append(volumeBuy).append(",").append(volume).append(",").append(open).append(",").append(close)
		.append(Utils.NEW_LINE);
		return builder.toString();
	}
	
}
