package com.jbescos.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.cloudchart.CandleChart;
import com.jbescos.common.CloudProperties;
import com.jbescos.exchange.FearGreedIndex;
import com.jbescos.exchange.Kline;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.Interval;
import com.jbescos.exchange.Utils;

public class PublicAPITest {

	@Test
	@Ignore
    public void allPrices() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        Map<String, Double> prices = publicAPI.priceKucoin();
        System.out.println("Kucoin " + prices.size() + " size: " + prices );
        prices = publicAPI.priceBinance();
        System.out.println("Binance " + prices.size() + " size: " + prices );
        prices = publicAPI.priceOkex();
        System.out.println("Okex " + prices.size() + " size: " + prices );
        prices = publicAPI.priceFtx();
        System.out.println("Ftx " + prices.size() + " size: " + prices );
        client.close();
	}
	
	@Test
	@Ignore
    public void getFearGreedIndex() {
		Client client = ClientBuilder.newClient();
		PublicAPI publicAPI = new PublicAPI(client);
		List<FearGreedIndex> fearGreed = publicAPI.getFearGreedIndex("2");
		System.out.println(fearGreed);
		client.close();
	}
	
	@Test
	@Ignore
	public void klines() throws FileNotFoundException, IOException {
		int limit = 150;
		Client client = ClientBuilder.newClient();
		PublicAPI publicAPI = new PublicAPI(client);
		Date from = Utils.getDateOfDaysBackZero(new Date(), limit);
		List<Kline> klines = publicAPI.klines(Interval.DAY_1, "BTCUSDT", limit, from.getTime(), null);
		System.out.println("Data from " + from.getTime() + ":" + klines);
		client.close();
    	File chartFile = new File("./target/klines.png");
        try (FileOutputStream output = new FileOutputStream(chartFile)) {
        	CandleChart chart = new CandleChart();
        	chart.add("BTCUSDT", klines);
        	chart.save(output, "BTC KLines", "", "USDT");
        }
	}
	
	@Test
	@Ignore
	public void compatibleSymbols() {
		Client client = ClientBuilder.newClient();
		PublicAPI publicAPI = new PublicAPI(client);
		Map<String, Double> pricesKucoin = publicAPI.priceKucoin();
		Map<String, Double> pricesBinance = publicAPI.priceBinance();
		Set<String> compatibleSymbols = new HashSet<>();
		for (String symbol : pricesBinance.keySet()) {
			if (pricesKucoin.containsKey(symbol)) {
				compatibleSymbols.add(symbol);
				if (compatibleSymbols.size() == 100) {
					break;
				}
			}
		}
		client.close();
		System.out.println("Compatible symbols: " + compatibleSymbols);
	}
	
	@Test
	@Ignore
	public void prices() {
		Client client = ClientBuilder.newClient();
		PublicAPI publicAPI = new PublicAPI(client);
		Map<String, Double> binancePrices = publicAPI.priceBinance();
		Map<String, Double> kucoinSellPrices = publicAPI.priceKucoin();
		Map<String, Double> kucoinBuyPrices = publicAPI.priceKucoin(ticker -> Double.parseDouble(ticker.getSell()));
		for (String symbol : new CloudProperties("binance", null).BOT_WHITE_LIST_SYMBOLS) {
			Double binanceSellPrice = binancePrices.get(symbol);
			Double kucoinSellPrice = kucoinSellPrices.get(symbol);
			Double kucoinBuyPrice = kucoinBuyPrices.get(symbol);
			if (binanceSellPrice != null)
				System.out.println("Binance price: " + Utils.format(binanceSellPrice) + " " + symbol);
			if (kucoinBuyPrice != null)
				System.out.println("Kucoin buy price: " + Utils.format(kucoinBuyPrice) + " " + symbol);
			if (kucoinSellPrice != null)
				System.out.println("Kucoin sell price: " + Utils.format(kucoinSellPrice) + " " + symbol);
		}
		client.close();
	}
}
