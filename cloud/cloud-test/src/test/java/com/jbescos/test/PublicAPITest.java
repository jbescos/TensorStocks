package com.jbescos.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.cloudchart.CandleChart;
import com.jbescos.common.FearGreedIndex;
import com.jbescos.common.Kline;
import com.jbescos.common.PublicAPI;
import com.jbescos.common.PublicAPI.Interval;
import com.jbescos.common.Utils;

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
	
}
