package com.jbescos.test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.Broker.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.SecuredKucoinAPI;

public class SecuredKucoinTest {

	private static final Logger LOGGER = Logger.getLogger(SecuredKucoinTest.class.getName());
	private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties();
	private static final Client client = ClientBuilder.newClient();
	
	@BeforeClass
	public static void beforeClass() {
		client.register(new LoggingFeature(LOGGER, Level.INFO, null, null));
	}
	
	@AfterClass
	public static void afterClass() {
		client.close();
	}

	@Test
	@Ignore
	public void wallet() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		// {"code":"200000","data":[{"id":"61726237e6f0340001e0c85b","currency":"USDT","type":"trade","balance":"1000","available":"1000","holds":"0"}]}
		SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
		System.out.println(api.wallet());
	}
	
	@Test
	@Ignore
	public void getOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		// Order Info: {id=618433b273c96c00019a6333, symbol=OPCT-USDT, opType=DEAL, type=market, side=buy, price=0, size=2, funds=0, dealFunds=0.28224, dealSize=2, fee=0.00028224, feeCurrency=USDT, stp=, stop=, stopTriggered=false, stopPrice=0, timeInForce=GTC, postOnly=false, hidden=false, iceberg=false, visibleSize=0, cancelAfter=0, channel=API, clientOid=278bbdf4-d337-436f-9d58-637ac8874461, remark=null, tags=null, isActive=false, cancelExist=false, createdAt=1636053938370, tradeType=TRADE}
		SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
		System.out.println(api.getOrder("6184220f35ed8b00017a4940"));
	}
	
	@Test
	@Ignore
	public void orderSymbol() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		// {"code":"200000","data":{"orderId":"6184220f35ed8b00017a4940"}}
		SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
		System.out.println(api.orderSymbol("VIDTUSDT", Action.BUY, "1", null));
	}
	
	@Test
	@Ignore
	public void orderUsdt() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		// {"code":"200000","data":{"orderId":"6184220f35ed8b00017a4940"}}
		SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
		System.out.println(api.orderUSDT("VIDTUSDT", Action.BUY, "2", null));
	}
}
