package com.jbescos.common;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

//https://telegram.rest/docs
public class TelegramBot {

    private static final Logger LOGGER = Logger.getLogger(TelegramBot.class.getName());
    private static final String BASE_URL = "https://api.telegram.org/bot";
    private final Client client;
    private final String userId;
    private final String url;
    private final String exceptionUrl;
    private final String chatId;
    private final String chartBotUrl;
    private final boolean enabled;

    public TelegramBot(TelegramInfo telegramInfo, Client client) {
    	this.enabled = telegramInfo.isEnabled();
        this.userId = telegramInfo.getUserId();
        this.url = BASE_URL + telegramInfo.getToken() + "/sendmessage";
        this.exceptionUrl = BASE_URL + telegramInfo.getExceptionToken() + "/sendmessage";
        this.client = client;
        this.chatId = telegramInfo.getChatId();
        this.chartBotUrl = telegramInfo.getChartBotUrl();
    }

    public TelegramBot(CloudProperties cloudProperties, Client client) {
    	this(new TelegramInfo(cloudProperties.TELEGRAM_BOT_ENABLED, cloudProperties.USER_ID, cloudProperties.TELEGRAM_BOT_TOKEN, cloudProperties.TELEGRAM_BOT_EXCEPTION_TOKEN, cloudProperties.TELEGRAM_CHAT_ID, cloudProperties.CHART_URL), client);
    }
    
    public void sendMessage(String text) {
    	sendMessage(text, url);
    }

    public void sendMessage(String text, String url) {
    	if (enabled) {
	        Map<String, String> message = new HashMap<>();
	        message.put("chat_id", chatId);
	        message.put("text", text);
	        message.put("method", "sendmessage");
	        WebTarget webTarget = client.target(url);
	        try (Response response = webTarget.request(MediaType.APPLICATION_JSON)
	                .header("charset", StandardCharsets.UTF_8.name())
	                .post(Entity.entity(message, MediaType.APPLICATION_JSON))) {
	            if (response.getStatus() != 200) {
	                LOGGER.warning("SecuredMizarAPI> HTTP response code " + response.getStatus() + " from " + webTarget.toString() + " with " + message + ": " + response.readEntity(String.class));
	            }
	        } catch (Exception e) {
	            LOGGER.log(Level.SEVERE, "Cannot send to telegram bot from " + webTarget.toString() + " with " + message, e);
	        }
    	}
    }

    public void sendChartSymbolLink(String symbol) {
        String msg = chartBotUrl + "?userId=" + userId + "&days=7&symbol=" + symbol + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg, url);
    }

    public void sendChartWalletDaysLink(int days) {
        String msg = chartBotUrl + "?userId=" + userId + "&days=" + days + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg, url);
    }

    public void sendChartSummaryLink(int days) {
        String msg = chartBotUrl + "?userId=" + userId + "&type=summary" + "&days=" + days + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg, url);
    }
    
    public void sendHtmlLink() {
    	String msg = chartBotUrl + "?userId=" + userId + "&type=html&uncache=" + System.currentTimeMillis();
        sendMessage(msg, url);
    }
    
    public void exception(String message, Exception e) {
    	sendMessage(message + ": " + e.getMessage(), exceptionUrl);
    }

}