package com.jbescos.localbot;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.exchange.PublicAPI;
import com.jbescos.localbot.BinanceWebSocket.BinanceMessage;
import com.jbescos.localbot.KucoinWebsocket.KucoinMessage;
import com.jbescos.localbot.PricesWorker.Price;

import jakarta.websocket.DeploymentException;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws InterruptedException, DeploymentException, IOException, URISyntaxException {
		ConcurrentHashMap<String, Price> binancePrices = new ConcurrentHashMap<>();
		Function<String, MessageWorker<BinanceMessage>> binanceWorker = symbol -> new PricesWorker<>(symbol, binancePrices);
		IWebsocket binanceSocket = new BinanceWebSocket(new MessageHandlerImpl<BinanceMessage>(BinanceMessage.class, binanceWorker), BinanceWebSocket.SUBSCRIPTION_BOOK_TICKER);
		ConcurrentHashMap<String, Price> kucoinPrices = new ConcurrentHashMap<>();
		Client client = ClientBuilder.newClient();
		PublicAPI publicAPI = new PublicAPI(client);
		Function<String, MessageWorker<KucoinMessage>> kucoinWorker = symbol -> new PricesWorker<>(symbol, kucoinPrices);
		IWebsocket kucoinSocket = new KucoinWebsocket(new MessageHandlerImpl<KucoinMessage>(KucoinMessage.class, kucoinWorker), client);
		binanceSocket.start();
		kucoinSocket.start();
		Map<String, Long> tempDisabledSymbols = new HashMap<>();
		while (true) {
			for (String symbol : Constants.SYMBOLS) {
				Price binancePrice = binancePrices.get(symbol);
				Price kucoinPrice = kucoinPrices.get(symbol);
				if (binancePrice == null || kucoinPrice == null) {
//					LOGGER.warning("No symbol found: " + symbol);
				} else {
					long delta = binancePrice.getTimestamp() - kucoinPrice.getTimestamp();
					if (delta < 2000 && delta > -2000 ) {
						double binanceSellPrice = Double.parseDouble(binancePrice.getSellPrice());
						double kucoinBuyPrice = Double.parseDouble(kucoinPrice.getBuyPrice());
						if (benefit(binanceSellPrice, kucoinBuyPrice)) {
							double profit = profitPercent(kucoinBuyPrice, binanceSellPrice);
							if (profit > Constants.PROFIT) {
								boolean trade = verifySymbolForTrading(publicAPI, tempDisabledSymbols, symbol);
								if (trade) {
									LOGGER.info(symbol + ": buy in Kucoin and sell in Binance. Profit " + format(profit, 8) + "%");
								}
							}
						}
						double binanceBuyPrice = Double.parseDouble(binancePrice.getBuyPrice());
						double kucoinSellPrice = Double.parseDouble(kucoinPrice.getSellPrice());
						if (benefit(kucoinSellPrice, binanceBuyPrice)) {
							double profit = profitPercent(binanceBuyPrice, kucoinSellPrice);
							if (profit > Constants.PROFIT) {
								boolean trade = verifySymbolForTrading(publicAPI, tempDisabledSymbols, symbol);
								if (trade) {
									LOGGER.info(symbol + ": buy in Binance and sell in Kucoin. Profit " + format(profit, 8) + "%");
								}
							}
						}
					}
				}
			}
		}
	}
	
	// Check whether the symbol is available for trading and it was not processed few minutes ago
	private static boolean verifySymbolForTrading(PublicAPI publicAPI, Map<String, Long> tempDisabledSymbols, String symbol) {
		long now = System.currentTimeMillis();
		Long timestamp = tempDisabledSymbols.get(symbol);
		if (timestamp == null) {
			boolean tradeable = publicAPI.isSymbolEnabled(symbol);
			tempDisabledSymbols.put(symbol, now);
			if (!tradeable) {
				LOGGER.info("Cannot trade with " + symbol + " because it is disabled in exchanges");
			}
			return tradeable;
		} else {
			if (now - timestamp > Constants.SYMBOL_VERIFICATION_TIME) {
				tempDisabledSymbols.put(symbol, now);
				boolean tradeable = publicAPI.isSymbolEnabled(symbol);
				return tradeable;
			} else {
				return false;
			}
		}
	}
	
	private static double profitPercent(double low, double high) {
		double diff = high - low;
		return diff * 100 / high;
	}
	
    private static String format(double amount, int digits) {
    	DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0", symbols);
        df.setRoundingMode(RoundingMode.DOWN);
        df.setMaximumFractionDigits(digits);
        return df.format(amount);
    }
	
	private static boolean benefit(double sellPrice, double buyPrice) {
		if (sellPrice > buyPrice) {
			return true;
		} else {
			return false;
		}
	}
}
