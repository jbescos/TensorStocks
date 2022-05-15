package com.jbescos.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class CsvProfitRow {
	
	public static final String PREFIX = "profit/profit_";
	public static final String HEADER = "SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISSION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%" + Utils.NEW_LINE;
	
	private final Date sellDate;
	private final Date firstBuyDate;
	private final String symbol;
	private final String quantityBuy;
	private final String quantityUsdtBuy;
	private final String quantitySell;
	private final String quantityUsdtSell;
	private final String commissionPercentage;
	private final String commissionUsdt;
	private final String usdtProfit;
	private final String netUsdtProfit;
	private final String profitPercentage;
	
	public CsvProfitRow(Date firstBuyDate, Date sellDate, String symbol, String quantityBuy, String quantityUsdtBuy,
			String quantitySell, String quantityUsdtSell, String commissionPercentage, String commissionUsdt,
			String usdtProfit, String netUsdtProfit, String profitPercentage) {
		this.firstBuyDate = firstBuyDate;
		this.sellDate = sellDate;
		this.symbol = symbol;
		this.quantityBuy = quantityBuy;
		this.quantityUsdtBuy = quantityUsdtBuy;
		this.quantitySell = quantitySell;
		this.quantityUsdtSell = quantityUsdtSell;
		this.commissionPercentage = commissionPercentage;
		this.commissionUsdt = commissionUsdt;
		this.usdtProfit = usdtProfit;
		this.netUsdtProfit = netUsdtProfit;
		this.profitPercentage = profitPercentage;
	}

	public String toCsvLine() {
		StringBuilder data = new StringBuilder();
		data.append(Utils.fromDate(Utils.FORMAT_SECOND, sellDate)).append(",").append(Utils.fromDate(Utils.FORMAT_SECOND, firstBuyDate)).append(",").append(symbol)
		.append(",").append(quantityBuy).append(",").append(quantitySell).append(",").append(quantityUsdtBuy).append(",").append(quantityUsdtSell).append(",").append(commissionPercentage)
		.append(",").append(commissionUsdt).append(",").append(usdtProfit).append(",").append(netUsdtProfit).append(",").append(profitPercentage).append(Utils.NEW_LINE);
		return data.toString();
	}
	
	public static CsvProfitRow build(String commission, TransactionsSummary summary, CsvTransactionRow sell) {
		if (!summary.getPreviousBuys().isEmpty()) {
			CsvTransactionRow firstBuy = summary.getPreviousBuys().get(summary.getPreviousBuys().size() - 1);
			Date firstBuyDate = firstBuy.getDate();
			Date sellDate = sell.getDate();
			String symbol = sell.getSymbol();
			BigDecimal d100 = new BigDecimal(100);
			BigDecimal quantityBuy = new BigDecimal("0");
			BigDecimal quantityUsdtBuy = new BigDecimal("0");
			for (CsvTransactionRow tx : summary.getPreviousBuys()) {
				quantityBuy = quantityBuy.add(new BigDecimal(tx.getQuantity()));
				quantityUsdtBuy = quantityUsdtBuy.add(new BigDecimal(tx.getUsdt()));
			}
			String quantitySell = sell.getQuantity();
			String quantityUsdtSell = sell.getUsdt();
			BigDecimal quantityUsdtSellbd = new BigDecimal(quantityUsdtSell);
			/*
			 *  Normalize SELL quantity to match the BUY quantity.
			 *  It could happen that the user did a purchase out of the system. It takes the current price from newest
			 */
			BigDecimal quantitySellbd = new BigDecimal(quantitySell);
			if (quantitySellbd.compareTo(quantityBuy) > 0) {
				quantitySell = Utils.format(quantityBuy, 2);
				/*
				 *  Recalculate USDT from new quantity with the current price.
				 */
				quantityUsdtSellbd = new BigDecimal(Utils.usdValue(quantityBuy.doubleValue(), sell.getUsdtUnit()));
			}
			quantityUsdtSell = Utils.format(quantityUsdtSellbd, 2);
			// FIXME What to do if he sold out of the system?.
			BigDecimal usdtProfit = quantityUsdtSellbd.subtract(quantityUsdtBuy);
			BigDecimal commissionUsdt = new BigDecimal(commission).multiply(usdtProfit);
			BigDecimal netUsdtProfit = usdtProfit.subtract(commissionUsdt);
			String commissionPercentage = Utils.format(new BigDecimal(commission).multiply(d100), 2) + "%";
			/*
			 * quantityUsdtBuy -> 100
			 * usdtProfit -> X
			 * 
			 * profitPercentage = usdtProfit * 100 / quantityUsdtBuy
			 */
			String profitPercentage = Utils.format(usdtProfit.multiply(d100).divide(quantityUsdtBuy, 8, RoundingMode.DOWN), 2) + "%";
			return new CsvProfitRow(firstBuyDate, sellDate, symbol, Utils.format(quantityBuy, 2), Utils.format(quantityUsdtBuy, 2), quantitySell, quantityUsdtSell,
					commissionPercentage, Utils.format(commissionUsdt, 2), Utils.format(usdtProfit, 2), Utils.format(netUsdtProfit, 2), profitPercentage);
		} else {
			return null;
		}
	}
	
	public String getSymbol() {
		return symbol;
	}

    public Date getSellDate() {
		return sellDate;
	}

	public String getQuantityUsdtBuy() {
		return quantityUsdtBuy;
	}

	public String getQuantityUsdtSell() {
		return quantityUsdtSell;
	}

	@Override
    public String toString() {
    	StringBuilder content = new StringBuilder(symbol).append(" SELL ").append(Utils.fromDate(Utils.FORMAT_SECOND, sellDate))
    	.append("\nFirst purchase: ").append(Utils.fromDate(Utils.FORMAT_SECOND, firstBuyDate))
    	.append("\nBuy / Sell: ").append(quantityUsdtBuy).append("$ / ").append(quantityUsdtSell).append("$")
    	.append("\nProfit: ").append(usdtProfit).append("$ (<b>").append(profitPercentage).append("</b>)");
    	if (profitPercentage.indexOf(0) == '-') {
    		content.append(" ❌");
    	} else {
    		content.append(" ✅");
    	}
        return content.toString();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commissionPercentage == null) ? 0 : commissionPercentage.hashCode());
		result = prime * result + ((commissionUsdt == null) ? 0 : commissionUsdt.hashCode());
		result = prime * result + ((firstBuyDate == null) ? 0 : firstBuyDate.hashCode());
		result = prime * result + ((netUsdtProfit == null) ? 0 : netUsdtProfit.hashCode());
		result = prime * result + ((profitPercentage == null) ? 0 : profitPercentage.hashCode());
		result = prime * result + ((quantityBuy == null) ? 0 : quantityBuy.hashCode());
		result = prime * result + ((quantitySell == null) ? 0 : quantitySell.hashCode());
		result = prime * result + ((quantityUsdtBuy == null) ? 0 : quantityUsdtBuy.hashCode());
		result = prime * result + ((quantityUsdtSell == null) ? 0 : quantityUsdtSell.hashCode());
		result = prime * result + ((sellDate == null) ? 0 : sellDate.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result + ((usdtProfit == null) ? 0 : usdtProfit.hashCode());
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
		CsvProfitRow other = (CsvProfitRow) obj;
		if (commissionPercentage == null) {
			if (other.commissionPercentage != null)
				return false;
		} else if (!commissionPercentage.equals(other.commissionPercentage))
			return false;
		if (commissionUsdt == null) {
			if (other.commissionUsdt != null)
				return false;
		} else if (!commissionUsdt.equals(other.commissionUsdt))
			return false;
		if (firstBuyDate == null) {
			if (other.firstBuyDate != null)
				return false;
		} else if (!firstBuyDate.equals(other.firstBuyDate))
			return false;
		if (netUsdtProfit == null) {
			if (other.netUsdtProfit != null)
				return false;
		} else if (!netUsdtProfit.equals(other.netUsdtProfit))
			return false;
		if (profitPercentage == null) {
			if (other.profitPercentage != null)
				return false;
		} else if (!profitPercentage.equals(other.profitPercentage))
			return false;
		if (quantityBuy == null) {
			if (other.quantityBuy != null)
				return false;
		} else if (!quantityBuy.equals(other.quantityBuy))
			return false;
		if (quantitySell == null) {
			if (other.quantitySell != null)
				return false;
		} else if (!quantitySell.equals(other.quantitySell))
			return false;
		if (quantityUsdtBuy == null) {
			if (other.quantityUsdtBuy != null)
				return false;
		} else if (!quantityUsdtBuy.equals(other.quantityUsdtBuy))
			return false;
		if (quantityUsdtSell == null) {
			if (other.quantityUsdtSell != null)
				return false;
		} else if (!quantityUsdtSell.equals(other.quantityUsdtSell))
			return false;
		if (sellDate == null) {
			if (other.sellDate != null)
				return false;
		} else if (!sellDate.equals(other.sellDate))
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		if (usdtProfit == null) {
			if (other.usdtProfit != null)
				return false;
		} else if (!usdtProfit.equals(other.usdtProfit))
			return false;
		return true;
	}
	
}
