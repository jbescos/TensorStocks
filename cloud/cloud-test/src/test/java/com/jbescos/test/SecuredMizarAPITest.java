package com.jbescos.test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.SecuredMizarAPI;
import com.jbescos.common.Utils;

public class SecuredMizarAPITest {

	private final Client client = ClientBuilder.newClient();
	private final SecuredMizarAPI mizarApi = SecuredMizarAPI.create(DataLoader.CLOUD_PROPERTIES, client);
	
	@Test
	@Ignore
	public void exchanges() {
		System.out.println(mizarApi.exchanges());
	}
	
	@Test
	@Ignore
	public void symbols() {
		List<String> compatibleSymbols = mizarApi.compatibleSymbols("kucoin", "SPOT");
		StringBuilder builder = new StringBuilder();
		for (String symbol : compatibleSymbols) {
			if (builder.length() == 0) {
				builder.append("bot.white.list=");
			} else {
				builder.append(",");
			}
			builder.append(symbol);
		}
		System.out.println(builder.toString());
	}

	@Test
	@Ignore
	public void createStrategy() {
		String name = "Jorge Test 2";
		String description = "Automated strategy creator";
		List<String> exchanges = Arrays.asList("kucoin");
		List<String> symbols = mizarApi.compatibleSymbols("kucoin", "SPOT");
		String market = "SPOT";
		int strategyId = mizarApi.publishSelfHostedStrategy(name, description, exchanges, symbols, market);
		System.out.println("StrategyId: " + strategyId);
	}

	@Test
	@Ignore
	public void serverTime() {
		System.out.println(Utils.fromDate(Utils.FORMAT_SECOND, new Date(mizarApi.serverTime())));
	}
	
	@Test
	@Ignore
	public void getOpenAllPositions() {
		System.out.println(mizarApi.getOpenAllPositions());
	}
	
	@Test
	@Ignore
	public void selfHostedStrategyInfo() {
		System.out.println(mizarApi.selfHostedStrategyInfo());
	}

}