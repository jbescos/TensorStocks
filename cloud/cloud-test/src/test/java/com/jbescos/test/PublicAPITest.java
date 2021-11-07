package com.jbescos.test;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.Price;
import com.jbescos.common.PublicAPI;

public class PublicAPITest {

	@Test
	@Ignore
    public void allPrices() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        List<Price> prices = publicAPI.priceKucoin();
        System.out.println("Kucoin " + prices.size() + " size: " + prices );
        prices = publicAPI.priceBinance();
        System.out.println("Binance " + prices.size() + " size: " + prices );
        prices = publicAPI.priceOkex();
        System.out.println("Okex " + prices.size() + " size: " + prices );
        prices = publicAPI.priceFtx();
        System.out.println("Ftx " + prices.size() + " size: " + prices );
        client.close();
	}
}
