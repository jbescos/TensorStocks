package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.CsvUtil;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.SecuredBinanceAPI;
import com.jbescos.exchange.SecuredKucoinAPI;
import com.jbescos.exchange.Utils;

public class SecuredBinanceAPITest {

	private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties(null);

	@Test
	@Ignore
	public void account() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Client client = ClientBuilder.newClient();
		SecuredBinanceAPI api = SecuredBinanceAPI.create(CLOUD_PROPERTIES, client);
		Map<String,String> wallet = api.wallet();
		Map<String, Double> prices = Exchange.BINANCE.price(new PublicAPI(client));
		List<Map<String, String>> rows = Utils.userUsdt(new Date(), prices, wallet);
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
	@Ignore
	public void address() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Client client = ClientBuilder.newClient();
		SecuredBinanceAPI api = SecuredBinanceAPI.create(CLOUD_PROPERTIES, client);
		System.out.println("Address:" + api.depositAddress("RLC"));
		client.close();
	}
	
	@Test
	@Ignore
	public void checkExploits() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		Client client = ClientBuilder.newClient();
		List<String> symbols = Arrays.asList("VIDTUSDT", "TKOUSDT", "PUNDIXUSDT", "LUNAUSDT", "HARDUSDT");
		SecuredBinanceAPI binance = SecuredBinanceAPI.create(CLOUD_PROPERTIES, client);
		Map<String, String> binanceWallet = binance.wallet();
		SecuredKucoinAPI kucoin = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
		Map<String, String> kucoinWallet = kucoin.wallet();
		for (String symbol : symbols) {
			try {
				System.out.println(symbol + " Wallet: " + kucoinWallet.get(symbol) + " Kucoin Address:" + kucoin.depositAddress(symbol));
			} catch (Exception e) {
				System.out.println(symbol + " Wallet: " + kucoinWallet.get(symbol) + " Kucoin Addresss: " + e.getMessage());
			}
			try {
				System.out.println(symbol + " Wallet: " + binanceWallet.get(symbol) + " Binance Address:" + binance.depositAddress(symbol));
			} catch (Exception e) {
				System.out.println(symbol + " Wallet: " + binanceWallet.get(symbol) + " Binance Address: " + e.getMessage());
			}
		}
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
