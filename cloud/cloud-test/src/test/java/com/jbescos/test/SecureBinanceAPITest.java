package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.Account;
import com.jbescos.common.PublicAPI;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Price;
import com.jbescos.common.SecuredBinanceAPI;
import com.jbescos.common.Utils;

public class SecureBinanceAPITest {

	private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties();

	@Test
	@Ignore
	public void account() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Client client = ClientBuilder.newClient();
		SecuredBinanceAPI api = SecuredBinanceAPI.create(CLOUD_PROPERTIES, client);
		Account account = api.account();
		List<Price> prices = new PublicAPI(client).price();
		List<Map<String, String>> rows = Utils.userUsdt(new Date(), prices, account);
		System.out.println(CsvUtil.toString(rows));
		client.close();
	}
	
	@Test
	@Ignore
	public void testOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Client client = ClientBuilder.newClient();
		SecuredBinanceAPI api = SecuredBinanceAPI.create(CLOUD_PROPERTIES, client);
		System.out.println(api.testOrder("DOGEUSDT", "BUY", "10"));
		client.close();
	}
	
	@Test
	public void signature() throws InvalidKeyException, NoSuchAlgorithmException {
		Client client = ClientBuilder.newClient();
		SecuredBinanceAPI api = SecuredBinanceAPI.create(client, "vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A", "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j");
		String signature = api.signature("symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559");
		assertEquals("c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71", signature);
		client.close();
	}
}
