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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hasTransactions ? 1231 : 1237);
		result = prime * result + ((lastPurchase == null) ? 0 : lastPurchase.hashCode());
		long temp;
		temp = Double.doubleToLongBits(lowestPurchase);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minProfitable);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((previousBuys == null) ? 0 : previousBuys.hashCode());
		result = prime * result + ((previousSells == null) ? 0 : previousSells.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransactionsSummary other = (TransactionsSummary) obj;
		if (hasTransactions != other.hasTransactions)
			return false;
		if (lastPurchase == null) {
			if (other.lastPurchase != null)
				return false;
		} else if (!lastPurchase.equals(other.lastPurchase))
			return false;
		if (Double.doubleToLongBits(lowestPurchase) != Double.doubleToLongBits(other.lowestPurchase))
			return false;
		if (Double.doubleToLongBits(minProfitable) != Double.doubleToLongBits(other.minProfitable))
			return false;
		if (previousBuys == null) {
			if (other.previousBuys != null)
				return false;
		} else if (!previousBuys.equals(other.previousBuys))
			return false;
		if (previousSells == null) {
			if (other.previousSells != null)
				return false;
		} else if (!previousSells.equals(other.previousSells))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TransactionsSummary [minProfitable=" + minProfitable + ", lowestPurchase=" + lowestPurchase
				+ ", hasTransactions=" + hasTransactions + ", lastPurchase=" + lastPurchase + ", previousBuys="
				+ previousBuys + ", previousSells=" + previousSells + "]";
	}

}
