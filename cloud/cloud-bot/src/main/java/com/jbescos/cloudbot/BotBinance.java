package com.jbescos.cloudbot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.jbescos.cloudbot.SymbolStats.Action;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

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
		if (updateWallet(Utils.USDT, buy * -1)) {
			api.testOrder(symbol, Action.BUY.name(), String.format("%.6f", buy));
		}
	}
	
	private void sell(String symbol, SymbolStats stat) throws FileNotFoundException, IOException {
		String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
		wallet.putIfAbsent(walletSymbol, 0.0);
		double unitsOfSymbol = wallet.get(walletSymbol);
		double sell = unitsOfSymbol * FACTOR * stat.getFactor();
		if (updateWallet(walletSymbol, sell * -1)) {
			double usdtSell = sell * stat.getNewest().getPrice();
			api.testOrder(symbol, Action.SELL.name(), String.format("%.6f", usdtSell));
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
