package com.jbescos.exchange;

import java.util.Date;

import com.jbescos.exchange.Broker.Action;

public class CsvTransactionRow implements IRow {

	private final Date date;
	private final String orderId;
	private final Action side;
	private final String symbol;
	private String usdt;
	private final String quantity;
	private final double usdtUnit;
	private double score = 0;
	private boolean sync = false;
	
	public CsvTransactionRow(Date date, String orderId, Action side, String symbol, String usdt, String quantity,
	        double usdtUnit) {
		this.date = date;
		this.orderId = orderId;
		this.side = side;
		this.symbol = symbol;
		this.usdt = usdt;
		this.quantity = quantity;
		this.usdtUnit = usdtUnit;
	}

	public CsvTransactionRow(CsvTransactionRow unsynced, String usdt, String quantity) {
		this.date = unsynced.getDate();
		this.orderId = unsynced.getOrderId();
		this.side = unsynced.getSide();
		this.symbol = unsynced.getSymbol();
		this.usdt = usdt;
		this.quantity = quantity;
		this.usdtUnit = Utils.usdUnitValue(Double.parseDouble(quantity), Double.parseDouble(usdt));
		this.score = unsynced.getScore();
		this.sync = true;
	}
			
	@Override
	public Date getDate() {
		return date;
	}

	public String getOrderId() {
		return orderId;
	}

	public Action getSide() {
		return side;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getUsdt() {
		return usdt;
	}

	public String getQuantity() {
		return quantity;
	}

	public double getUsdtUnit() {
		return usdtUnit;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public boolean isSync() {
		return sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	@Override
    public String toString() {
    	StringBuilder content = new StringBuilder().append("ORDER ID: ").append(orderId).append("\n").append(symbol).append(" BUY ").append(Utils.fromDate(Utils.FORMAT_SECOND, date))
    	    	.append("\nTotal USD (USD per unit): ").append(usdt).append("$ (").append(Utils.format(usdtUnit, 2)).append("$) 💵");
    	return content.toString();
    }

	public String toCsvLine() {
	    StringBuilder data = new StringBuilder();
        data.append(Utils.fromDate(Utils.FORMAT_SECOND, date)).append(",").append(orderId).append(",").append(side.name()).append(",").append(symbol).append(",").append(usdt).append(",").append(quantity).append(",").append(Utils.format(usdtUnit));
        data.append(",").append(Utils.format(score));
        data.append(",").append(sync);
        data.append("\r\n");
		return data.toString();
	}

	@Override
	public double getPrice() {
		return Double.parseDouble(usdt);
	}

	public void setUsdt(double usdt) {
		this.usdt = Double.toString(usdt);
	}

	@Override
	public String getLabel() {
		return side.name() + "-" + symbol;
	}

	@Override
	public Double getAvg() {
		return null;
	}

	@Override
	public Double getAvg2() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((orderId == null) ? 0 : orderId.hashCode());
		result = prime * result + ((quantity == null) ? 0 : quantity.hashCode());
		result = prime * result + ((side == null) ? 0 : side.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result + ((usdt == null) ? 0 : usdt.hashCode());
		long temp;
		temp = Double.doubleToLongBits(usdtUnit);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		CsvTransactionRow other = (CsvTransactionRow) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (orderId == null) {
			if (other.orderId != null)
				return false;
		} else if (!orderId.equals(other.orderId))
			return false;
		if (quantity == null) {
			if (other.quantity != null)
				return false;
		} else if (!quantity.equals(other.quantity))
			return false;
		if (side != other.side)
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		if (usdt == null) {
			if (other.usdt != null)
				return false;
		} else if (!usdt.equals(other.usdt))
			return false;
		if (Double.doubleToLongBits(usdtUnit) != Double.doubleToLongBits(other.usdtUnit))
			return false;
		return true;
	}

}
