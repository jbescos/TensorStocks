package com.jbescos.common;

import java.util.Date;
import java.util.List;

public class TransactionsSummary {

	private final double minProfitable;
	private final double lowestPurchase;
	private final boolean hasTransactions;
	private final Date lastPurchase;
	private final List<CsvTransactionRow> previousBuys;
	private final List<CsvTransactionRow> previousSells;
	
	public TransactionsSummary(boolean hasTransactions, double minProfitable, double lowestPurchase, Date lastPurchase, List<CsvTransactionRow> previousBuys, List<CsvTransactionRow> previousSells) {
		this.hasTransactions = hasTransactions;
		this.minProfitable = minProfitable;
		this.lowestPurchase = lowestPurchase;
		this.lastPurchase = lastPurchase;
		this.previousBuys = previousBuys;
		this.previousSells = previousSells;
	}

	public boolean isHasTransactions() {
		return hasTransactions;
	}

	public double getMinProfitable() {
		return minProfitable;
	}

	public double getLowestPurchase() {
        return lowestPurchase;
    }

    public Date getLastPurchase() {
		return lastPurchase;
	}

	public List<CsvTransactionRow> getPreviousBuys() {
		return previousBuys;
	}

	public List<CsvTransactionRow> getPreviousSells() {
		return previousSells;
	}

}
