package com.jbescos.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.PublicAPI;

public class KucoinTest {

	@Test
	@Ignore
    public void allPrices() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        System.out.println(publicAPI.priceKucoin());
        client.close();
	}
}
