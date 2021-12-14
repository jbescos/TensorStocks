package com.jbescos.common;

public interface Broker {

	Action getAction();
	
	CsvRow getNewest();
	
	String getSymbol();
	
	double getFactor();
	
	TransactionsSummary getPreviousTransactions();
	
	public static enum Action {
		BUY {
            @Override
            public String side() {
                return "BUY";
            }
        }, SELL {
            @Override
            public String side() {
                return "SELL";
            }
        }, NOTHING {
            @Override
            public String side() {
                throw new IllegalStateException("Cannot operate with Action.NOTHING");
            }
        };
	    
	    public abstract String side();
	}
}
