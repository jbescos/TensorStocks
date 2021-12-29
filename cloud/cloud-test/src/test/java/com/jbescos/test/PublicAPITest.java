package com.jbescos.test;

import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.FearGreedIndex;
import com.jbescos.common.PublicAPI;

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
}
