package es.tododev.stocks.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PriceItem {

	private long date; // Timestamp
	private double open;
	private double high;
	private double low;
	private double close;
	private long volume;
	private double adjclose;
	@JsonIgnore
	// Does not come from REST rquest, but it is populated by us
	private int symbol;

	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public double getOpen() {
		return open;
	}
	public void setOpen(double open) {
		this.open = open;
	}
	public double getHigh() {
		return high;
	}
	public void setHigh(double high) {
		this.high = high;
	}
	public double getLow() {
		return low;
	}
	public void setLow(double low) {
		this.low = low;
	}
	public double getClose() {
		return close;
	}
	public void setClose(double close) {
		this.close = close;
	}
	public long getVolume() {
		return volume;
	}
	public void setVolume(long volume) {
		this.volume = volume;
	}
	public double getAdjclose() {
		return adjclose;
	}
	public void setAdjclose(double adjclose) {
		this.adjclose = adjclose;
	}
	public int getSymbol() {
		return symbol;
	}
	public void setSymbol(int symbol) {
		this.symbol = symbol;
	}

}
