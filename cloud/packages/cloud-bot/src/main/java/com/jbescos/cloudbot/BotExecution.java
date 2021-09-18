package com.jbescos.cloudbot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jbescos.common.Broker;
import com.jbescos.common.Broker.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

public class BotExecution {
	
	private static final Logger LOGGER = Logger.getLogger(BotExecution.class.getName());
	private static final double FLOAT_ISSUE = 0.999999;
	private final ConnectAPI connectAPI;
	private final Map<String, Double> wallet;
	private final CloudProperties cloudProperties;
	
	private BotExecution(CloudProperties cloudProperties, ConnectAPI connectAPI) {
		this.cloudProperties = cloudProperties;
		this.connectAPI = connectAPI;
		this.wallet = connectAPI.wallet();
	}
	
	public void execute(List<Broker> stats) throws IOException  {
		for (Broker stat : stats) {
			LOGGER.info(() -> "Processing " + stat);
			if (stat.getAction() == Action.BUY) {
				buy(stat.getSymbol(), stat);
			} else if (stat.getAction() == Action.SELL || stat.getAction() == Action.SELL_PANIC) {
				sell(stat.getSymbol(), stat);
			}
		}
		connectAPI.postActions(stats);
	}
	
	private void buy(String symbol, Broker stat) throws FileNotFoundException, IOException {
	    if (!cloudProperties.BOT_NEVER_BUY_LIST_SYMBOLS.contains(symbol)) {
	    	try {
		    	String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
	    		wallet.putIfAbsent(Utils.USDT, 0.0);
	    		double usdt = wallet.get(Utils.USDT);
	    		double buy = usdt * cloudProperties.BOT_BUY_REDUCER;
	    		if (!cloudProperties.BOT_BUY_IGNORE_FACTOR_REDUCER) {
	    		    buy = buy * stat.getFactor();
	    		}
	    		if (buy < connectAPI.minTransaction()) {
	    			buy = connectAPI.minTransaction();
	    		}
	    		double buySymbol = Utils.symbolValue(buy, stat.getNewest().getPrice());
	    		LOGGER.info("Trying to buy " + Utils.format(buy) + " " + Utils.USDT + ". Stats = " + stat);
	    		if (updateWallet(Utils.USDT, buy * -1)) {
	    				connectAPI.order(symbol, stat, Utils.format(buySymbol), Utils.format(buy));
	    				wallet.putIfAbsent(walletSymbol, 0.0);
	    				updateWallet(walletSymbol, Utils.applyCommission(buySymbol, cloudProperties.BOT_BUY_COMMISSION));
	    		}
	    	} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot buy " + symbol, e);
			}
	    } else {
	        LOGGER.info(() -> symbol + " discarded to buy because it is in bot.never.buy");
	    }
	}
	
	private void sell(String symbol, Broker stat) throws FileNotFoundException, IOException {
		try {
			String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
			wallet.putIfAbsent(walletSymbol, 0.0);
			// Make sure we sell a little less than what we have
			double sell = wallet.get(walletSymbol) * FLOAT_ISSUE;
			double usdtOfSymbol = Utils.usdValue(sell, stat.getNewest().getPrice());
			if (usdtOfSymbol >= connectAPI.minTransaction()) {
				LOGGER.info(() -> "Selling " + Utils.format(usdtOfSymbol) + " " + Utils.USDT);
				if (updateWallet(walletSymbol, sell * -1)) {
					connectAPI.order(symbol, stat, Utils.format(sell), Utils.format(usdtOfSymbol));
					updateWallet(Utils.USDT, Utils.applyCommission(usdtOfSymbol, cloudProperties.BOT_SELL_COMMISSION));
				} else {
					LOGGER.warning("Error with the wallet. It is expected to sell " + Utils.format(sell) + " " + symbol + " and there is " + Utils.format(wallet.get(walletSymbol)));
				}
			} else {
				LOGGER.info(() -> "Cannot sell " + Utils.format(usdtOfSymbol) + " " + Utils.USDT + " of " + symbol + " because it is lower than " + Utils.format(connectAPI.minTransaction()));
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Cannot sell " + symbol, e);
		}
	}

	private boolean updateWallet(String symbol, double amount) {
		double current = wallet.get(symbol);
		double accumulated = current + amount;
		if (accumulated > 0) {
			wallet.put(symbol, accumulated);
			return true;
		} else {
			LOGGER.info(() -> "There is not enough money in the wallet. There is only " + Utils.format(current) + symbol);
		}
		return false;
	}
	
	public static BotExecution binance(CloudProperties cloudProperties, SecureBinanceAPI api) {
		return new BotExecution(cloudProperties, new Binance(cloudProperties, api));
	}
	
	public static BotExecution test(CloudProperties cloudProperties, Map<String, Double> wallet, List<CsvTransactionRow> transactions, List<CsvRow> walletHistorical) {
		return new BotExecution(cloudProperties, new Test(wallet, transactions, walletHistorical));
	}
	
	private static interface ConnectAPI {
		
		Map<String, Double> wallet();
		
		void order(String symbol, Broker stat, String quantity, String quantityUsd);

		double minTransaction();
		
		void postActions(List<Broker> stats);
	}
	
	private static class Binance implements ConnectAPI {
		
		private final SecureBinanceAPI api;
		private final CloudProperties cloudProperties;
		private final Map<String, String> originalWallet;
		private final Map<String, Double> wallet = new HashMap<>();
		
		private Binance(CloudProperties cloudProperties, SecureBinanceAPI api) {
			this.cloudProperties = cloudProperties;
			this.api = api;
			this.originalWallet = api.wallet();
			for (Entry<String, String> entry : originalWallet.entrySet()) {
				wallet.put(entry.getKey(), Double.parseDouble(entry.getValue()));
			}
		}

		@Override
		public Map<String, Double> wallet() {
			return wallet;
		}

		@Override
		public void order(String symbol, Broker stat, String quantity, String quantityUsd) {
			try {
				if (stat.getAction() == Action.SELL || stat.getAction() == Action.SELL_PANIC) {
					String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
					api.orderSymbol(symbol, stat.getAction(), originalWallet.get(walletSymbol));
				} else {
					api.orderSymbol(symbol, stat.getAction(), quantity);
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot " + stat.getAction().name() + " " + quantity + " " + symbol, e);
			}
		}

		@Override
		public double minTransaction() {
			return cloudProperties.BINANCE_MIN_TRANSACTION;
		}

		@Override
		public void postActions(List<Broker> stats) {}
		
	}
	
	private static class Test implements ConnectAPI {
		
		private final Map<String, Double> wallet;
		private final List<CsvTransactionRow> transactions;
		private final List<CsvRow> walletHistorical;
		private final double minTransaction;
		
		private Test(Map<String, Double> wallet, List<CsvTransactionRow> transactions, List<CsvRow> walletHistorical) {
			this.wallet = wallet;
			this.transactions = transactions;
			this.walletHistorical = walletHistorical;
			// Min transaction is 1/10 of initial money
			this.minTransaction = wallet.get(Utils.USDT) * 0.1;
		}

		@Override
		public Map<String, Double> wallet() {
			return wallet;
		}

		@Override
		public void order(String symbol, Broker stat, String quantity, String quantityUsd) {
			// FIXME apply commission here?
			CsvTransactionRow transaction = new CsvTransactionRow(stat.getNewest().getDate(), UUID.randomUUID().toString(), stat.getAction(), symbol, Double.parseDouble(quantityUsd), Double.parseDouble(quantity), stat.getNewest().getPrice());
			transactions.add(transaction);
		}

		@Override
		public double minTransaction() {
			return minTransaction;
		}

		private double usdtSnappshot(List<Broker> stats) {
			double snapshot = wallet.get(Utils.USDT);
			for (Broker stat : stats) {
				Double amount = wallet.get(stat.getSymbol().replaceFirst(Utils.USDT, ""));
				if (amount != null) {
					snapshot = snapshot + Utils.usdValue(amount, stat.getNewest().getPrice());
				}
			}
			return snapshot;
		}

		@Override
		public void postActions(List<Broker> stats) {
			double usdtSnapshot = usdtSnappshot(stats);
			CsvRow newest = stats.get(0).getNewest();
			CsvRow walletUsdt = new CsvRow(newest.getDate(), "WALLET-TOTAL-" + Utils.USDT, usdtSnapshot, null, null);
			walletHistorical.add(walletUsdt);
		}
		
	}

}
