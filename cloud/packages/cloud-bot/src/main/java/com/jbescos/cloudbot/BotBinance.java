package com.jbescos.cloudbot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.Utils;
import com.jbescos.common.SymbolStats.Action;

public class BotBinance {

	private static final Logger LOGGER = Logger.getLogger(BotBinance.class.getName());
	// Sell or buy only 20% of what is available
	private static final double FACTOR = 0.2;
	private static final List<String> WHITE_LIST_SYMBOLS; 
	private final SecureBinanceAPI api;
	private final Map<String, Double> wallet;
	
	static {
		try {
			Properties properties = Utils.fromClasspath("/bot.properties");
			WHITE_LIST_SYMBOLS = Arrays.asList(properties.getProperty("white.list").split(","));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public BotBinance(SecureBinanceAPI api) {
		this.api = api;
		this.wallet = api.wallet();
	}
	
	public void execute(List<SymbolStats> stats) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		for (SymbolStats stat : stats) {
			if (WHITE_LIST_SYMBOLS == null || WHITE_LIST_SYMBOLS.contains(stat.getSymbol())) {
				if (stat.getAction() == Action.BUY) {
					buy(stat.getSymbol(), stat);
				} else if (stat.getAction() == Action.SELL) {
					sell(stat.getSymbol(), stat);
				}
			}
		}
	}
	
	private void buy(String symbol, SymbolStats stat) throws FileNotFoundException, IOException {
		wallet.putIfAbsent(Utils.USDT, 0.0);
		double usdt = wallet.get(Utils.USDT);
		double buy = usdt * FACTOR * stat.getFactor();
		LOGGER.info("Trying to buy " + buy + " of " + usdt + " USDT. Stats = " + stat);
		if (updateWallet(Utils.USDT, buy * -1)) {
			try {
				api.order(symbol, Action.BUY.name(), String.format(Locale.US, "%.6f", buy));
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot buy " + symbol, e);
			}
		}
	}
	
	private void sell(String symbol, SymbolStats stat) throws FileNotFoundException, IOException {
		String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
		wallet.putIfAbsent(walletSymbol, 0.0);
		double unitsOfSymbol = wallet.get(walletSymbol);
		double sell = unitsOfSymbol * FACTOR * stat.getFactor();
		LOGGER.info("Trying to sell " + sell + " of " + unitsOfSymbol + " " + symbol + ". Stats = " + stat);
		if (updateWallet(walletSymbol, sell * -1)) {
			double usdtSell = sell * stat.getNewest().getPrice();
			try {
				api.order(symbol, Action.SELL.name(), String.format(Locale.US, "%.6f", usdtSell));
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot sell " + symbol, e);
			}
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
}
