package com.jbescos.cloudbot;

import java.util.List;
import java.util.Map;

import com.jbescos.cloudbot.SymbolStats.Action;
import com.jbescos.common.Utils;

public class Bot {

	// Sell or buy only 20% of what is available
	private static final double FACTOR = 0.2;
	private final Map<String, Double> wallet;
	private final List<String> whiteListSymbols; 
	private final boolean skip;
	private double usdtSnapshot;
	private boolean didAction;
	
	public Bot(Map<String, Double> wallet, boolean skip, List<String> whiteListSymbols) {
		this.wallet = wallet;
		this.skip = skip;
		this.whiteListSymbols = whiteListSymbols;
		wallet.putIfAbsent(Utils.USDT, 0.0);
	}
	
	public Bot(Map<String, Double> wallet, boolean skip) {
		this(wallet, skip, null);
	}
	
	public void execute(List<SymbolStats> stats) {
		didAction = false;
		if (!skip) {
			for (SymbolStats stat : stats) {
				if (whiteListSymbols == null || whiteListSymbols.contains(stat.getSymbol())) {
					if (stat.getAction() == Action.BUY) {
						buy(stat.getSymbol(), stat);
					} else if (stat.getAction() == Action.SELL) {
						sell(stat.getSymbol(), stat);
					}
				}
			}
		}
		this.usdtSnapshot = usdtSnappshot(stats);
	}
	
	public Map<String, Double> getWallet() {
		return wallet;
	}

	private double usdtSnappshot(List<SymbolStats> stats) {
		double snapshot = wallet.get(Utils.USDT);
		for (SymbolStats stat : stats) {
			Double amount = wallet.get(stat.getSymbol());
			if (amount != null) {
				snapshot = snapshot + (amount * stat.getNewest().getPrice());
			}
		}
		return snapshot;
	}
	
	private void buy(String symbol, SymbolStats stat) {
		double currentPrice = stat.getNewest().getPrice();
		wallet.putIfAbsent(symbol, 0.0);
		double usdt = wallet.get(Utils.USDT);
		double buy = usdt * FACTOR;
		if (updateWallet(Utils.USDT, buy * -1)) {
			double unitsOfSymbol = buy / (currentPrice + (currentPrice * Utils.BUY_COMISSION));
//			double unitsOfSymbol = buy / currentPrice;
			updateWallet(symbol, unitsOfSymbol);
			didAction = true;
//			System.out.println("Buying " + unitsOfSymbol + " " + symbol + " and spent " + buy + " USDT. 1 " + symbol + " = " + currentPrice + " USDT. Avg = " + stat.getAvg());
		}
	}
	
	private void sell(String symbol, SymbolStats stat) {
		double currentPrice = stat.getNewest().getPrice();
		wallet.putIfAbsent(symbol, 0.0);
		double unitsOfSymbol = wallet.get(symbol);
		double sell = unitsOfSymbol * FACTOR;
		if (updateWallet(symbol, sell * -1)) {
			double usdt = currentPrice * sell;
			updateWallet(Utils.USDT, usdt);
			didAction = true;
//			System.out.println("Selling " + sell + " " + symbol + " and obtained " + usdt + " USDT. 1 " + symbol + " = " + currentPrice + " USDT. Avg = " + stat.getAvg());
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

	@Override
	public String toString() {
		return usdtSnapshot + " USDT -> " + wallet;
	}
	
}
