package com.jbescos.common;

public class Kline {

	private final long openTime;
	private final String open;
	private final String high;
	private final String low;
	private final String close;
	private final String volume;
	private final long closeTime;
	private final String quoteAssetVolume;
	private final int numerOfTrades;
	private final String takerBuyBaseAssetVolume;
	private final String takerBuyQuoteAssetVolume;

	public Kline(long openTime, String open, String high, String low, String close, String volume, long closeTime,
			String quoteAssetVolume, int numerOfTrades, String takerBuyBaseAssetVolume,
			String takerBuyQuoteAssetVolume) {
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

	public static Kline fromArray(Object[] values) {
		return new Kline((long) values[0], (String) values[1], (String) values[2], (String) values[3],
				(String) values[4], (String) values[5], (long) values[6], (String) values[7], (int) values[8],
				(String) values[9], (String) values[10]);
	}

	@Override
	public String toString() {
		return Utils.NEW_LINE + "Kline [openTime=" + openTime + ", open=" + open + ", high=" + high + ", low=" + low + ", close=" + close
				+ ", volume=" + volume + ", closeTime=" + closeTime + ", quoteAssetVolume=" + quoteAssetVolume
				+ ", numerOfTrades=" + numerOfTrades + ", takerBuyBaseAssetVolume=" + takerBuyBaseAssetVolume
				+ ", takerBuyQuoteAssetVolume=" + takerBuyQuoteAssetVolume + "]";
	}

}
