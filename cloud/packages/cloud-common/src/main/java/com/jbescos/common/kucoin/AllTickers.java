package com.jbescos.common.kucoin;

import java.util.Collections;
import java.util.List;

public class AllTickers {

	private String code;
	private Data data;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public Data getData() {
		return data;
	}
	public void setData(Data data) {
		this.data = data;
	}

	public static class Data {
		private long time;
		private List<Ticker> ticker = Collections.emptyList();
		public long getTime() {
			return time;
		}
		public void setTime(long time) {
			this.time = time;
		}
		public List<Ticker> getTicker() {
			return ticker;
		}
		public void setTicker(List<Ticker> ticker) {
			this.ticker = ticker;
		}
	}
	
	public static class Ticker {
		private String symbol;
		private String symbolName;
		private String buy;
		private String sell;
		private String changeRate;
		private String changePrice;
		private String high;
		private String low;
		private String vol;
		private String volValue;
		private String last;
		private String averagePrice;
		private String takerFeeRate;
		private String makerFeeRate;
		private String takerCoefficient;
		private String makerCoefficient;
		public String getSymbol() {
			return symbol;
		}
		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}
		public String getSymbolName() {
			return symbolName;
		}
		public void setSymbolName(String symbolName) {
			this.symbolName = symbolName;
		}
		public String getBuy() {
			return buy;
		}
		public void setBuy(String buy) {
			this.buy = buy;
		}
		public String getSell() {
			return sell;
		}
		public void setSell(String sell) {
			this.sell = sell;
		}
		public String getChangeRate() {
			return changeRate;
		}
		public void setChangeRate(String changeRate) {
			this.changeRate = changeRate;
		}
		public String getChangePrice() {
			return changePrice;
		}
		public void setChangePrice(String changePrice) {
			this.changePrice = changePrice;
		}
		public String getHigh() {
			return high;
		}
		public void setHigh(String high) {
			this.high = high;
		}
		public String getLow() {
			return low;
		}
		public void setLow(String low) {
			this.low = low;
		}
		public String getVol() {
			return vol;
		}
		public void setVol(String vol) {
			this.vol = vol;
		}
		public String getVolValue() {
			return volValue;
		}
		public void setVolValue(String volValue) {
			this.volValue = volValue;
		}
		public String getLast() {
			return last;
		}
		public void setLast(String last) {
			this.last = last;
		}
		public String getAveragePrice() {
			return averagePrice;
		}
		public void setAveragePrice(String averagePrice) {
			this.averagePrice = averagePrice;
		}
		public String getTakerFeeRate() {
			return takerFeeRate;
		}
		public void setTakerFeeRate(String takerFeeRate) {
			this.takerFeeRate = takerFeeRate;
		}
		public String getMakerFeeRate() {
			return makerFeeRate;
		}
		public void setMakerFeeRate(String makerFeeRate) {
			this.makerFeeRate = makerFeeRate;
		}
		public String getTakerCoefficient() {
			return takerCoefficient;
		}
		public void setTakerCoefficient(String takerCoefficient) {
			this.takerCoefficient = takerCoefficient;
		}
		public String getMakerCoefficient() {
			return makerCoefficient;
		}
		public void setMakerCoefficient(String makerCoefficient) {
			this.makerCoefficient = makerCoefficient;
		}
		
		
	}
}
