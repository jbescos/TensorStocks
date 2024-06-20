package com.jbescos.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.News;
import com.jbescos.exchange.Utils;

public class NewsUtils {

    private static final Logger LOGGER = Logger.getLogger(NewsUtils.class.getName());

    public static Map<Exchange, List<News>> news(long millis, PublicAPI publicAPI, CloudProperties cloudProperties, Client client, List<Exchange> exchanges) {
        Map<Exchange, List<News>> exchangeNews = new HashMap<>();
        try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
            long minutes30Back = millis - (30 * 60 * 1000);
            for (Exchange exchange : exchanges) {
                try {
                    List<News> news = exchange.news(publicAPI, minutes30Back);
                    if (!news.isEmpty()) {
                        LOGGER.info("Notifying " + news.size() + " news for " + exchange.name());
                        exchangeNews.put(exchange, news);
                    }
                    for (News n : news) {
                        telegram.exception(n.toString(), null);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Cannot obtain news from " + exchange.name(), e);
                    telegram.exception("Cannot obtain news from " + exchange.name(), e);
                }
            }
        }
        return exchangeNews;
    }

    public static Map<Exchange, List<News>> news(long millis, PublicAPI publicAPI, CloudProperties cloudProperties, Client client) {
        return news(millis, publicAPI, cloudProperties, client, Arrays.asList(Exchange.values()));
    }
    
    public static void saveNews(FileManager storage, Map<Exchange, List<News>> news) {
        String event = "DELIST";
        for (Entry<Exchange, List<News>> entry : news.entrySet()) {
            StringBuilder builder = new StringBuilder();
            for (News value : entry.getValue()) {
                String date = Utils.fromDate(Utils.FORMAT_SECOND, value.getDate());
                String url = value.getUrl();
                for (String symbol : value.getDelistedSymbols()) {
                    builder.append(date).append(",").append(event).append(",").append(symbol).append(",").append(url).append(Utils.NEW_LINE);
                }
            }
            String fileName = "data" + entry.getKey().getFolder() + Utils.NEWS_SUBFIX;
            try {
                storage.updateFile(fileName, builder.toString().getBytes(), News.HEAD.getBytes());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Cannot save " + fileName, e);
            }
        }
    }

    public static List<String> delisted(FileManager storage, Exchange exchange) {
        String fileName = "data" + exchange.getFolder() + Utils.NEWS_SUBFIX;
        List<String> delisted = storage.loadRows(fileName, reader -> CsvUtil.delisted(reader));
        LOGGER.info(() -> "Delisted symbols: " + delisted + " from " + fileName);
        return delisted;
    }
}
