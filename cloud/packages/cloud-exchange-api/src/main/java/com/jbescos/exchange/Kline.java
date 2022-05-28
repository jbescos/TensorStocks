package com.jbescos.exchange;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Kline {

	
	private final long openTime;
	private final String openTimeStr;
	private final String open;
	private final String high;
	private final String low;
	private final String close;
	private final String volume;
	private final long closeTime;
	private final String closeTimeStr;
	private final String quoteAssetVolume;
	private final int numerOfTrades;
	private final String takerBuyBaseAssetVolume;
	private final String takerBuyQuoteAssetVolume;
	private final String symbol;
	private final List<String> supportList = new ArrayList<>();
	private final List<String> resistancetList = new ArrayList<>();

	public Kline(long openTime, String open, String high, String low, String close, String volume, long closeTime,
			String quoteAssetVolume, int numerOfTrades, String takerBuyBaseAssetVolume,
			String takerBuyQuoteAssetVolume, String symbol) {
		this.openTime = openTime;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
		this.closeTime = closeTime;
		this.quoteAssetVolume = quoteAssetVolume;
		this.numerOfTrades = numerOfTrades;
		this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
		this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
		this.symbol = symbol;
		this.openTimeStr = Utils.fromDate(Utils.FORMAT, new Date(openTime));
		this.closeTimeStr = Utils.fromDate(Utils.FORMAT, new Date(closeTime));
	}

	public long getOpenTime() {
		return openTime;
	}

	public String getOpen() {
		return open;
	}

	public String getHigh() {
		return high;
	}

	public String getLow() {
		return low;
	}

	public String getClose() {
		return close;
	}

	public String getVolume() {
		return volume;
	}

	public long getCloseTime() {
		return closeTime;
	}

	public String getQuoteAssetVolume() {
		return quoteAssetVolume;
	}

	public int getNumerOfTrades() {
		return numerOfTrades;
	}

	public String getTakerBuyBaseAssetVolume() {
		return takerBuyBaseAssetVolume;
	}

	public String getTakerBuyQuoteAssetVolume() {
		return takerBuyQuoteAssetVolume;
	}

	public String getOpenTimeStr() {
		return openTimeStr;
	}

	public String getCloseTimeStr() {
		return closeTimeStr;
	}

	public String getSymbol() {
		return symbol;
	}

	public static Kline fromArray(String symbol, Object[] values) {
		return new Kline((long) values[0], (String) values[1], (String) values[2], (String) values[3],
				(String) values[4], (String) values[5], (long) values[6], (String) values[7], (int) values[8],
				(String) values[9], (String) values[10], symbol);
	}

	@Override
	public String toString() {
		return "Kline [openTime=" + openTime + ", openTimeStr=" + openTimeStr + ", open=" + open + ", high=" + high
				+ ", low=" + low + ", close=" + close + ", volume=" + volume + ", closeTime=" + closeTime
				+ ", closeTimeStr=" + closeTimeStr + ", quoteAssetVolume=" + quoteAssetVolume + ", numerOfTrades="
				+ numerOfTrades + ", takerBuyBaseAssetVolume=" + takerBuyBaseAssetVolume + ", takerBuyQuoteAssetVolume="
				+ takerBuyQuoteAssetVolume + ", symbol=" + symbol + "]";
	}

	private String toString(List<String> list) {
		StringBuilder builder = new StringBuilder();
		for (String value : list) {
			if (builder.length() != 0) {
				builder.append("-");
			}
			builder.append(value);
		}
		return builder.toString();
	}

	public String toCsv() {
//		OPEN_TIME,CLOSE_TIME,SYMBOL,HIGH,LOW,OPEN,CLOSE,VOLUME,ASSET_VOLUME,SUPPORT_LIST,RESISTANCE_LIST
		StringBuilder builder = new StringBuilder();
		builder.append(openTime).append(",").append(closeTime).append(",").append(symbol).append(",").append(high).append(",").append(low)
		.append(",").append(open).append(",").append(close).append(",").append(volume).append(",").append(quoteAssetVolume)
		.append(",").append(toString(supportList)).append(",").append(toString(resistancetList)).append(Utils.NEW_LINE);
		return builder.toString();
	}
}
