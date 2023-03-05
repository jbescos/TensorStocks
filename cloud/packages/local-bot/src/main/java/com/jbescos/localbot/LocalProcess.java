package com.jbescos.localbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.jbescos.common.BotProcess;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.FileStorage;
import com.jbescos.common.NewsUtils;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.FearGreedIndex;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.Utils;

public class LocalProcess {

    private static final Logger LOGGER = Logger.getLogger(LocalProcess.class.getName());
    private static final String PROPERTIES_PATH = "./crypto-properties/";
    private final ExecutorService executor;
    private final Client client;
    private final FileStorage storage;

    public LocalProcess(ExecutorService executor, Client client, FileStorage storage) {
        this.executor = executor;
        this.client = client;
        this.storage = storage;
    }

    public void run() throws IOException {
        Date now = new Date();
        Map<String, List<CloudProperties>> usersByExchange = new HashMap<>();
        CloudProperties mainProperties = storage.loadMainProperties(PROPERTIES_PATH);
        List<CloudProperties> properties = storage.loadProperties(PROPERTIES_PATH);
        for (CloudProperties user : properties) {
            List<CloudProperties> usersByFolder = usersByExchange.get(user.USER_EXCHANGE.getFolder());
            if (usersByFolder == null) {
                usersByFolder = new ArrayList<>();
                usersByExchange.put(user.USER_EXCHANGE.getFolder(), usersByFolder);
            }
            usersByFolder.add(user);
        }
        
        String fileName = Utils.fromDate(Utils.FORMAT, now) + ".csv";
        PublicAPI publicAPI = new PublicAPI(client);
        NewsUtils.news(now.getTime(), publicAPI, mainProperties, client);

        FearGreedIndex fearGreedIndex = publicAPI.getFearGreedIndex("1").get(0);
        Set<String> updatedExchanges = new HashSet<>();
        for (Exchange exchange : Exchange.values()) {
            try {
                if (exchange.enabled() && updatedExchanges.add(exchange.getFolder())) {
                    String lastPrice = "data" + exchange.getFolder() + Utils.LAST_PRICE;
                    Map<String, CsvRow> previousRows = storage.previousRows(lastPrice);
                    Map<String, Price> prices = exchange.price(publicAPI);
                    List<CsvRow> updatedRows = storage.updatedRowsAndSaveLastPrices(previousRows, prices, now,
                            lastPrice, fearGreedIndex.getValue());
                    StringBuilder builder = new StringBuilder();
                    for (CsvRow row : updatedRows) {
                        builder.append(row.toCsvLine());
                    }
                    storage.updateFile("data" + exchange.getFolder() + fileName,
                            builder.toString().getBytes(Utils.UTF8), Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8));
                    if (usersByExchange.containsKey(exchange.getFolder())) {
                        for (CloudProperties user : usersByExchange.get(exchange.getFolder())) {
                            executor.submit(() -> {
                                BotProcess process = new BotProcess(user, client, storage);
                                try {
                                    process.execute();
                                } catch (Exception e) {
                                    LOGGER.log(Level.SEVERE, "Error executing bot for " + user.USER_ID, e);
                                }
                            });
                        }
                    } else {
                        LOGGER.info("There are no users for " + exchange.getFolder());
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Cannot process " + exchange.name(), e);
            }
        }
    }
}
