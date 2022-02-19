package com.jbescos.cloudbot;

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

import com.jbescos.common.CloudProperties;

public class TelegramBot {

    private static final Logger LOGGER = Logger.getLogger(TelegramBot.class.getName());
    private static final String BASE_URL = "https://api.telegram.org/bot";
    private final Client client;
    private final String userId;
    private final String url;
    private final String chatId;
    private final String chartBotUrl;

    public TelegramBot(CloudProperties cloudProperties, Client client) {
        this.userId = cloudProperties.USER_ID;
        this.url = BASE_URL + cloudProperties.TELEGRAM_BOT_TOKEN + "/sendmessage";
        this.client = client;
        this.chatId = cloudProperties.TELEGRAM_CHAT_ID;
        this.chartBotUrl = cloudProperties.CHART_URL;
    }

    public void sendMessage(String text) {
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

    public void sendChartSymbolLink(String symbol) {
        String msg = chartBotUrl + "?userId=" + userId + "&days=7&symbol=" + symbol + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg);
    }

    public void sendChartWalletDaysLink(int days) {
        String msg = chartBotUrl + "?userId=" + userId + "&days=" + days + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg);
    }

    public void sendChartSummaryLink(int days) {
        String msg = chartBotUrl + "?userId=" + userId + "&type=summary" + "&days=" + days + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg);
    }
    
    public void sendHtmlLink() {
    	String msg = chartBotUrl + "?userId=" + userId + "&type=html&uncache=" + System.currentTimeMillis();
        sendMessage(msg);
    }
    
}
