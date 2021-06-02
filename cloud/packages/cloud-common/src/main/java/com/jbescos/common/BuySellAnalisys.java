package com.jbescos.common;

public interface BuySellAnalisys {

	Action getAction();
	
	CsvRow getNewest();
	
	String getSymbol();
	
	double getFactor();
	
	public static enum Action {
		BUY, SELL, NOTHING;
	}
}
