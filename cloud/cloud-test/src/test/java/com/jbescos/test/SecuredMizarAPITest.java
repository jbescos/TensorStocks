package com.jbescos.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.exchange.SecuredMizarAPI;
import com.jbescos.exchange.Utils;
import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.SecuredMizarAPI.OpenPositionResponse;
import com.jbescos.exchange.SecuredMizarAPI.OpenPositions;
import com.jbescos.common.CloudProperties;

public class SecuredMizarAPITest {

	private final Client client = ClientBuilder.newClient();
	private final SecuredMizarAPI mizarApi = SecuredMizarAPI.create(new CloudProperties(null), client);
	private static final String EXCHANGE = "kucoin";
//	private static final String EXCHANGE = "binance";
	
	@Test
	@Ignore
	public void exchanges() {
		System.out.println(mizarApi.exchanges());
	}
	
	@Test
	@Ignore
	public void symbols() {
		List<String> compatibleSymbols = mizarApi.compatibleSymbols(EXCHANGE, "SPOT");
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
	public void createStrategyAll() {
		String name = "Botijo-Pijo-All";
		String description = "Bot will try to buy cheap and will sell with some profit. It is recommended to invest 1% each time, for example if you have 100.000$, invest 1.000$ in each transaction. It will never sell losses. In worst scenario, the bot will not have enough USDT to continue buying, in this case try to add more USDT or sell something.";
		List<String> exchanges = mizarApi.exchanges();
		Set<String> symbols = new HashSet<>();
		for (String exchange : exchanges) {
			symbols.addAll(mizarApi.compatibleSymbols(exchange, "SPOT"));
		}
		String market = "SPOT";
		int strategyId = mizarApi.publishSelfHostedStrategy(name, description, exchanges, new ArrayList<>(symbols), market);
		System.out.println("StrategyId: " + strategyId);
	}
	
	@Test
	@Ignore
	public void createStrategy() {
		String name = "Botijo-Pijo";
		String description = "Bot will try to buy cheap and will sell with some profit. It is recommended to invest 1% each time, for example if you have 100.000$, invest 1.000$ in each transaction. It will never sell losses. In worst scenario, the bot will not have enough USDT to continue buying, in this case try to add more USDT or sell something.";
		List<String> exchanges = Arrays.asList(EXCHANGE);
		List<String> symbols = mizarApi.compatibleSymbols(EXCHANGE, "SPOT");
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
		// {"open_positions":[{"position_id":"454","strategy_id":"113","open_timestamp":1634914483529,"open_price":"10.882000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":false}]}
		System.out.println(mizarApi.getOpenAllPositions());
	}
	
	@Test
	@Ignore
	public void selfHostedStrategyInfo() {
		System.out.println(mizarApi.selfHostedStrategyInfo());
	}
	
	@Test
	@Ignore
	public void openPosition() {
		// buy
		// open_price is the price of 1 unit in USDT
		// {"position_id":"452","strategy_id":"113","open_timestamp":1634913294278,"open_price":"10.843000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":false}
		System.out.println(mizarApi.openPosition("UNFI", "USDT", 0.1));
	}

	@Test
	@Ignore
	public void closePosition() {
		// sell
		// {"position_id":"453","strategy_id":"113","open_timestamp":1634913995389,"close_timestamp":1634914090671,"open_price":"10.827000000000","close_price":"10.843000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":false}
		System.out.println(mizarApi.closePosition(452));
	}

    @Test
    @Ignore
    public void closePositionAll() {
        OpenPositions openPositions = mizarApi.getOpenAllPositions();
        for (OpenPositionResponse position : openPositions.open_positions) {
            System.out.println(mizarApi.closePosition(position.position_id));
        }
    }
    
    @Test
    @Ignore
    public void closePositionAllBySymbol() {
    	mizarApi.openPosition("UNFI", "USDT", 0.1);
    	mizarApi.openPosition("UNFI", "USDT", 0.1);
    	System.out.println(mizarApi.getOpenAllPositions());
    	System.out.println(mizarApi.sell("UNFIUSDT", Action.SELL));
    	// {"closed_positions":[{"position_id":"769","strategy_id":"113","open_timestamp":1635404904035,"close_timestamp":1635405060644,"open_price":"10.822000000000","close_price":"10.822000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":true},{"position_id":"770","strategy_id":"113","open_timestamp":1635404911672,"close_timestamp":1635405060663,"open_price":"10.822000000000","close_price":"10.822000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":true}]}
//    	System.out.println(mizarApi.closeAllBySymbol("UNFIUSDT"));
    }

}
