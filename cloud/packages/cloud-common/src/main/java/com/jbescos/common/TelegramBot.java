package com.jbescos.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Utils;

//https://telegram.rest/docs
public class TelegramBot implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(TelegramBot.class.getName());
    private static final String BASE_URL = "https://api.telegram.org/bot";
    private static final int LIMIT_MESSAGE = 4096;
    private final Client client;
    private final String userId;
    private final String url;
    private final String exceptionUrl;
    private final String chatId;
    private final String chartBotUrl;
    private final Map<String, List<String>> bufferedMessages = new HashMap<>();

    public TelegramBot(TelegramInfo telegramInfo, Client client) {
        this.userId = telegramInfo.getUserId();
        if (!Utils.EMPTY_STR.equals(telegramInfo.getToken())) {
            this.url = BASE_URL + telegramInfo.getToken() + "/sendmessage";
        } else {
            this.url = null;
        }
        if (!Utils.EMPTY_STR.equals(telegramInfo.getExceptionToken())) {
            this.exceptionUrl = BASE_URL + telegramInfo.getExceptionToken() + "/sendmessage";
        } else {
            this.exceptionUrl = null;
        }
        this.client = client;
        this.chatId = telegramInfo.getChatId();
        this.chartBotUrl = telegramInfo.getChartBotUrl();
    }

    public TelegramBot(CloudProperties cloudProperties, Client client) {
        this(new TelegramInfo(cloudProperties.USER_ID, cloudProperties.TELEGRAM_BOT_TOKEN,
                cloudProperties.TELEGRAM_BOT_EXCEPTION_TOKEN, cloudProperties.TELEGRAM_CHAT_ID,
                cloudProperties.CHART_URL), client);
    }

    public void sendMessage(String text) {
        sendMessage(text, url);
    }

    public void sendMessage(String text, String url) {
        if (url != null) {
            List<String> messages = bufferedMessages.get(url);
            if (messages == null) {
                messages = new ArrayList<>();
                bufferedMessages.put(url, messages);
            }
            messages.add(text);
        }
    }

    public void sendChartSymbolLink(String symbol) {
        String msg = chartBotUrl + "?userId=" + userId + "&days=7&symbol=" + symbol + "&uncache="
                + System.currentTimeMillis();
        sendMessage(msg, url);
    }

    public void sendChartWalletDaysLink(int days) {
        String msg = chartBotUrl + "?userId=" + userId + "&days=" + days + "&uncache=" + System.currentTimeMillis();
        sendMessage(msg, url);
    }

    public void sendChartSummaryLink(int days) {
        String msg = chartBotUrl + "?userId=" + userId + "&type=summary" + "&days=" + days + "&uncache="
                + System.currentTimeMillis();
        sendMessage(msg, url);
    }

    public void sendHtmlLink() {
        String msg = chartBotUrl + "?userId=" + userId + "&type=html&uncache=" + System.currentTimeMillis();
        sendMessage(msg, url);
    }

    public void exception(String message, Exception e) {
        String exceptionMessage = e != null ? e.getMessage() : "";
        sendMessage(message + ": " + exceptionMessage, exceptionUrl);
    }

    public void flush() {
        for (Entry<String, List<String>> entry : bufferedMessages.entrySet()) {
            String url = entry.getKey();
            StringBuilder text = new StringBuilder();
            Map<String, String> message = new HashMap<>();
            message.put("chat_id", chatId);
            message.put("method", "sendmessage");
            if (exceptionUrl.equals(url)) {
                if (userId != null) {
                    text.insert(0, userId + "\n");
                } else {
                    text.insert(0, "Report\n");
                }
            } else {
                message.put("parse_mode", "html");
                if (userId != null) {
                    text.insert(0, "<b>📢 " + userId + "</b>\n");
                } else {
                    text.insert(0, "<b>📢 </b>\n");
                }
            }
            for (String msg : entry.getValue()) {
                text.append(msg).append("\n");
            }
            List<String> contents = Utils.splitSize(text.toString(), LIMIT_MESSAGE);
            for (String content : contents) {
                message.put("text", content);
                WebTarget webTarget = client.target(url);
                try (Response response = webTarget.request(MediaType.APPLICATION_JSON)
                        .header("charset", StandardCharsets.UTF_8.name())
                        .post(Entity.entity(message, MediaType.APPLICATION_JSON))) {
                    if (response.getStatus() != 200) {
                        LOGGER.warning("SecuredMizarAPI> HTTP response code " + response.getStatus() + " from "
                                + webTarget.toString() + " with " + message + ": " + response.readEntity(String.class));
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE,
                            "Cannot send to telegram bot from " + webTarget.toString() + " with " + message, e);
                }
            }
        }
    }

    @Override
    public void close() {
        flush();
    }
}
