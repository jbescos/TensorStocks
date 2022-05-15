package com.jbescos.test;

import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.TelegramBot;

public class TelegramBotTest {

	private final CloudProperties properties = new CloudProperties(null);

	@Test
//	@Ignore
	public void sendMessage() {
		try (TelegramBot bot = new TelegramBot(properties, ClientBuilder.newClient())) {
			bot.sendMessage("XSRUSDT SELL 2022-05-15 07:30:26\n" + 
					"First purchase: 2022-05-11 23:30:30\n" + 
					"Buy / Sell: 30$ / 66.49$\n" + 
					"Profit: 36.49$ (121.63%) ✅");
			bot.sendMessage("FORESTPLUSUSDT SELL 2022-05-15 07:30:29\n" + 
					"First purchase: 2022-05-12 03:00:36\n" + 
					"Buy / Sell: 30$ / 31.08$\n" + 
					"Profit: 1.08$ (3.61%) ❌");
		}
	}
	
	@Test
	@Ignore
	public void exception() {
		try (TelegramBot bot = new TelegramBot(properties, ClientBuilder.newClient())) {
			bot.exception("test", new Exception("exception"));
		}
	}
}
