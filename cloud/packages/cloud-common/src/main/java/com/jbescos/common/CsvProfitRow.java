package com.jbescos.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class CsvProfitRow {
	
	public static final String PREFIX = "profit/profit_";
	public static final String HEADER = "SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%" + Utils.NEW_LINE;
	
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
	
	private CsvProfitRow(Date firstBuyDate, Date sellDate, String symbol, String quantityBuy, String quantityUsdtBuy,
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
			quantitySell = Utils.format(quantityBuy);
			/*
			 *  Recalculate USDT from new quantity with the current price.
			 */
			quantityUsdtSellbd = new BigDecimal(Utils.usdValue(quantityBuy.doubleValue(), sell.getUsdtUnit()));
			quantityUsdtSell = Utils.format(quantityUsdtSellbd);
		}
		// FIXME What to do if he sold out of the system?.
		BigDecimal usdtProfit = quantityUsdtSellbd.subtract(quantityUsdtBuy);
		BigDecimal commissionUsdt = new BigDecimal(commission).multiply(usdtProfit);
		BigDecimal netUsdtProfit = usdtProfit.subtract(commissionUsdt);
		String commissionPercentage = Utils.format(new BigDecimal(commission).multiply(d100)) + "%";
		/*
		 * quantityUsdtBuy -> 100
		 * usdtProfit -> X
		 * 
		 * profitPercentage = usdtProfit * 100 / quantityUsdtBuy
		 */
		String profitPercentage = Utils.format(usdtProfit.multiply(d100).divide(quantityUsdtBuy, 8, RoundingMode.DOWN)) + "%";
		return new CsvProfitRow(firstBuyDate, sellDate, symbol, Utils.format(quantityBuy), Utils.format(quantityUsdtBuy), quantitySell, quantityUsdtSell,
				commissionPercentage, Utils.format(commissionUsdt), Utils.format(usdtProfit), Utils.format(netUsdtProfit), profitPercentage);
	}
}
