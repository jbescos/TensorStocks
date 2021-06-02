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
			if (CloudProperties.BOT_WHITE_LIST_SYMBOLS == null || CloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(stat.getSymbol())) {
				LOGGER.info("Processing " + stat);
				if (stat.getAction() == Action.BUY) {
					buy(stat.getSymbol(), stat);
				} else if (stat.getAction() == Action.SELL) {
					sell(stat.getSymbol(), stat);
				}
			}
		}
	}
	
	private void buy(String symbol, BuySellAnalisys stat) throws FileNotFoundException, IOException {
		wallet.putIfAbsent(Utils.USDT, 0.0);
		double usdt = wallet.get(Utils.USDT);
		double buy = usdt * CloudProperties.BOT_BUY_REDUCER * stat.getFactor();
		LOGGER.info("Trying to buy " + buy + " of " + usdt + " USDT. Stats = " + stat);
		if (updateWallet(Utils.USDT, buy * -1)) {
			try {
				api.order(symbol, Action.BUY.name(), Utils.format(buy));
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Cannot buy " + symbol, e);
			}
		}
	}
	
	private void sell(String symbol, BuySellAnalisys stat) throws FileNotFoundException, IOException {
		String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
		wallet.putIfAbsent(walletSymbol, 0.0);
		double unitsOfSymbol = wallet.get(walletSymbol);
		double sell = unitsOfSymbol * CloudProperties.BOT_SELL_REDUCER * stat.getFactor();
		double usdtSell = sell * stat.getNewest().getPrice();
		if (usdtSell < CloudProperties.BINANCE_MIN_TRANSACTION) {
			usdtSell = CloudProperties.BINANCE_MIN_TRANSACTION;
			sell = usdtSell / stat.getNewest().getPrice();
		}
		LOGGER.info("Trying to sell " + sell + " of " + unitsOfSymbol + " " + symbol + ". Stats = " + stat);
		if (updateWallet(walletSymbol, sell * -1)) {
			try {
				api.order(symbol, Action.SELL.name(), Utils.format(usdtSell));
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
