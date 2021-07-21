package com.jbescos.cloudbot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jbescos.common.BuySellAnalisys;
import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

public class BotBinance {

	private static final Logger LOGGER = Logger.getLogger(BotBinance.class.getName());
	private final SecureBinanceAPI api;
	private final Map<String, Double> wallet;
	
	public BotBinance(SecureBinanceAPI api) {
		this.api = api;
		this.wallet = api.wallet();
	}
	
	public void execute(List<BuySellAnalisys> stats) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		for (BuySellAnalisys stat : stats) {
			LOGGER.info("Processing " + stat);
			if (stat.getAction() == Action.BUY) {
				buy(stat.getSymbol(), stat);
			} else if (stat.getAction() == Action.SELL) {
				sell(stat.getSymbol(), stat);
			}
		}
	}
	
	private void buy(String symbol, BuySellAnalisys stat) throws FileNotFoundException, IOException {
		wallet.putIfAbsent(Utils.USDT, 0.0);
		double usdt = wallet.get(Utils.USDT);
		double buy = usdt * CloudProperties.BOT_BUY_REDUCER;
		if (!CloudProperties.BOT_BUY_IGNORE_FACTOR_REDUCER) {
		    buy = buy * stat.getFactor();
		}
		if (buy < CloudProperties.BINANCE_MIN_TRANSACTION) {
			buy = CloudProperties.BINANCE_MIN_TRANSACTION;
		}
		LOGGER.info("Trying to buy " + buy + " of " + usdt + " USDT. Stats = " + stat);
		if (updateWallet(Utils.USDT, buy * -1)) {
			try {
				api.orderUSDT(symbol, Action.BUY.name(), Utils.format(buy));
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot buy " + symbol, e);
			}
		}
	}
	
	private void sell(String symbol, BuySellAnalisys stat) throws FileNotFoundException, IOException {
		try {
			String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
			wallet.putIfAbsent(walletSymbol, 0.0);
			double unitsOfSymbol = wallet.get(walletSymbol);
			double usdtOfSymbol = unitsOfSymbol * stat.getNewest().getPrice();
			if (usdtOfSymbol >= CloudProperties.BINANCE_MIN_TRANSACTION) {
				double sell = unitsOfSymbol * CloudProperties.BOT_SELL_REDUCER;
				if (!CloudProperties.BOT_SELL_IGNORE_FACTOR_REDUCER) {
		            sell = sell * stat.getFactor();
		        }
				double usdtSell = sell * stat.getNewest().getPrice();
				boolean sellFlag = true;
				if ((usdtOfSymbol - usdtSell) < (CloudProperties.BINANCE_MIN_TRANSACTION * 2)) {
					// Sell everything
					LOGGER.info("Selling everything " + unitsOfSymbol + " " + symbol + " because it costs " + Utils.format(usdtOfSymbol) + " " + Utils.USDT);
					api.orderSymbol(symbol, Action.SELL.name(), Utils.format(unitsOfSymbol)); // Do not use the normal format because for example in SHIB it fails
					sellFlag = false;
				} else if (usdtSell < CloudProperties.BINANCE_MIN_TRANSACTION) {
					usdtSell = CloudProperties.BINANCE_MIN_TRANSACTION;
					sell = usdtSell / stat.getNewest().getPrice();
				}
				if (sellFlag) {
					LOGGER.info("Trying to sell " + sell + " of " + unitsOfSymbol + " " + symbol + ". Stats = " + stat);
					if (updateWallet(walletSymbol, sell * -1)) {
						api.orderUSDT(symbol, Action.SELL.name(), Utils.format(usdtSell));
						updateWallet(Utils.USDT, usdtSell);
					}
				}
			} else {
				LOGGER.info("Cannot sell " + Utils.format(usdtOfSymbol) + " " + Utils.USDT + " of " + symbol + " because it is lower than " + CloudProperties.BINANCE_MIN_TRANSACTION);
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
			LOGGER.info("There is not enough money in the wallet. There is only " + Utils.format(current) + symbol);
		}
		return false;
	}
}
