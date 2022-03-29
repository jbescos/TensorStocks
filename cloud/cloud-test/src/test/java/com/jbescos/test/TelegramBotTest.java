package com.jbescos.test;

import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.TelegramBot;

public class TelegramBotTest {

	private final CloudProperties properties = new CloudProperties(null);

	@Test
	@Ignore
	public void sendMessage() {
		TelegramBot bot = new TelegramBot(properties, ClientBuilder.newClient());
		bot.sendMessage("📢  Hello world");
	}
	
	@Test
	@Ignore
	public void exception() {
		TelegramBot bot = new TelegramBot(properties, ClientBuilder.newClient());
		bot.exception("test", new Exception("exception"));
	}
}
