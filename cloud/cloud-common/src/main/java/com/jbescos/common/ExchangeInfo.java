package com.jbescos.common;

public class ExchangeInfo {

	private String timezone;
	private long serverTime;
	private Object[] rateLimits;
	private Object[] exchangeFilters;
	private Object[] symbols;

	public String getTimezone() {
		return timezone;
	}
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}
	public long getServerTime() {
		return serverTime;
	}
	public void setServerTime(long serverTime) {
		this.serverTime = serverTime;
	}
	public Object[] getRateLimits() {
		return rateLimits;
	}
	public void setRateLimits(Object[] rateLimits) {
		this.rateLimits = rateLimits;
	}
	public Object[] getExchangeFilters() {
		return exchangeFilters;
	}
	public void setExchangeFilters(Object[] exchangeFilters) {
		this.exchangeFilters = exchangeFilters;
	}
	public Object[] getSymbols() {
		return symbols;
	}
	public void setSymbols(Object[] symbols) {
		this.symbols = symbols;
	}
	
	
	
}
