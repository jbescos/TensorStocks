package com.jbescos.test;

import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.cloudbot.TelegramBot;
import com.jbescos.common.CloudProperties;

public class TelegramBotTest {

	private final CloudProperties properties = new CloudProperties();

	@Test
	@Ignore
	public void sendMessage() {
		TelegramBot bot = new TelegramBot(properties, ClientBuilder.newClient());
		bot.sendMessage("Hello world");
	}
}
