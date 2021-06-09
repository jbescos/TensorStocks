package com.jbescos.cloudbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.jbescos.common.BuySellAnalisys;
import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.Utils;

public class Bot {

	private static final Logger LOGGER = Logger.getLogger(Bot.class.getName());
	private final Map<String, Double> wallet;
	private final boolean skip;
	private double usdtSnapshot;
	private boolean didAction;
	private final List<CsvTransactionRow> transactions = new ArrayList<>();
	private final List<CsvRow> walletHistorical = new ArrayList<>();

	public Bot(Map<String, Double> wallet, boolean skip) {
		this.wallet = wallet;
		this.skip = skip;
		wallet.putIfAbsent(Utils.USDT, 0.0);
	}

	public void execute(List<BuySellAnalisys> stats) {
		didAction = false;
		if (!skip) {
			for (BuySellAnalisys stat : stats) {
				if (stat.getAction() == Action.BUY) {
					buy(stat.getSymbol(), stat);
				} else if (stat.getAction() == Action.SELL) {
					sell(stat.getSymbol(), stat);
				}
			}
		}
		this.usdtSnapshot = usdtSnappshot(stats);
		CsvRow newest = stats.get(0).getNewest();
		CsvRow walletUsdt = new CsvRow(newest.getDate(), "WALLET-TOTAL-" + Utils.USDT, usdtSnapshot, null);
		walletHistorical.add(walletUsdt);
	}

	public Map<String, Double> getWallet() {
		return wallet;
	}

	private double usdtSnappshot(List<BuySellAnalisys> stats) {
		double snapshot = wallet.get(Utils.USDT);
		for (BuySellAnalisys stat : stats) {
			Double amount = wallet.get(stat.getSymbol());
			if (amount != null) {
				snapshot = snapshot + (amount * stat.getNewest().getPrice());
			}
		}
		return snapshot;
	}

	private void buy(String symbol, BuySellAnalisys stat) {
		double currentPrice = stat.getNewest().getPrice();
		wallet.putIfAbsent(symbol, 0.0);
		double usdt = wallet.get(Utils.USDT);
		double buy = usdt * CloudProperties.BOT_BUY_REDUCER * stat.getFactor();
		if (updateWallet(Utils.USDT, buy * -1)) {
			double unitsOfSymbol = buy / (currentPrice + (currentPrice * CloudProperties.BOT_BUY_COMISSION));
//			double unitsOfSymbol = buy / currentPrice;
			updateWallet(symbol, unitsOfSymbol);
			CsvTransactionRow transaction = new CsvTransactionRow(stat.getNewest().getDate(), UUID.randomUUID().toString(), Action.BUY, symbol, buy, unitsOfSymbol, currentPrice);
			transactions.add(transaction);
			didAction = true;
//			LOGGER.info(stat + "" + transaction);
		}
	}

	private void sell(String symbol, BuySellAnalisys stat) {
		double currentPrice = stat.getNewest().getPrice();
		wallet.putIfAbsent(symbol, 0.0);
		double unitsOfSymbol = wallet.get(symbol);
		double sell = unitsOfSymbol * CloudProperties.BOT_SELL_REDUCER * stat.getFactor();
		if (updateWallet(symbol, sell * -1)) {
			double usdt = currentPrice * sell;
			usdt = usdt - (usdt * CloudProperties.BOT_SELL_COMISSION);
			updateWallet(Utils.USDT, usdt);
			CsvTransactionRow transaction = new CsvTransactionRow(stat.getNewest().getDate(), UUID.randomUUID().toString(), Action.SELL, symbol, usdt, unitsOfSymbol, currentPrice);
			transactions.add(transaction);
			didAction = true;
//			LOGGER.info(stat + "" + transaction);
		}

	}

	private boolean updateWallet(String symbol, double amount) {
		double accumulated = wallet.get(symbol) + amount;
		if (accumulated > 0) {
			wallet.put(symbol, accumulated);
			return true;
		}
		return false;
	}

	public double getUsdtSnapshot() {
		return usdtSnapshot;
	}

	public boolean isDidAction() {
		return didAction;
	}

	public List<CsvTransactionRow> getTransactions() {
		return transactions;
	}

	public List<CsvRow> getWalletHistorical() {
		return walletHistorical;
	}

	@Override
	public String toString() {
		return usdtSnapshot + " USDT -> " + wallet;
	}

}
