package com.jbescos.cloudstorage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.api.gax.paging.Page;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.Blob;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.FileManager;
import com.jbescos.common.PropertiesConfigurationException;
import com.jbescos.common.PublisherMgr;
import com.jbescos.common.StorageInfo;
import com.jbescos.common.TelegramBot;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.FearGreedIndex;
import com.jbescos.exchange.Kline;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.Interval;
import com.jbescos.exchange.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

    private static final Logger LOGGER = Logger.getLogger(StorageFunction.class.getName());
    private static final byte[] CSV_HEADER_TOTAL = Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8);
    private static final String SKIP_PARAM = "skip";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        boolean skip = Boolean.parseBoolean(Utils.getParam(SKIP_PARAM, "false", request.getQueryParameters()));
        StorageInfo storageInfo = StorageInfo.build();
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        BucketStorage storage = new BucketStorage(storageInfo);

        Map<String, List<String>> groupedFolder = new HashMap<>();
        Page<Blob> files = storage.list(storageInfo.getPropertiesBucket());
        for (Blob blob : files.iterateAll()) {
            if (blob.isDirectory()) {
                String userId = blob.getName().replaceAll("/", "");
                try {
                    CloudProperties user = new CloudProperties(userId, storageInfo);
                    if (user.USER_ACTIVE) {
                        List<String> usersByFolder = groupedFolder.get(user.USER_EXCHANGE.getFolder());
                        if (usersByFolder == null) {
                            usersByFolder = new ArrayList<>();
                            groupedFolder.put(user.USER_EXCHANGE.getFolder(), usersByFolder);
                        }
                        usersByFolder.add(userId);
                    } else {
                        LOGGER.log(Level.WARNING, userId + " is not active. Skipping notifications.");
                    }
                } catch (PropertiesConfigurationException e) {
                    try (TelegramBot telegram = new TelegramBot(e.getTelegramInfo(), client)) {
                        LOGGER.log(Level.SEVERE, "Cannot load user properties of " + userId, e);
                        telegram.exception("Cannot load user properties of " + userId, e);
                    }
                }
            }
        }
        Date now = new Date();
        LOGGER.info(() -> "Server time is: " + Utils.fromDate(Utils.FORMAT_SECOND, now));

        CloudProperties cloudProperties = new CloudProperties(storageInfo);

        FearGreedIndex fearGreedIndex = publicAPI.getFearGreedIndex("1").get(0);
        try (PublisherMgr publisher = PublisherMgr.create(cloudProperties)) {
            if (!skip) {
                Set<String> updatedExchanges = new HashSet<>();
                for (Exchange exchange : Exchange.values()) {
                    if (exchange.enabled()) {
                        if (updatedExchanges.add(exchange.getFolder())) {
                            try {
                                String lastPrice = "data" + exchange.getFolder() + Utils.LAST_PRICE;
                                Map<String, CsvRow> previousRows = storage.previousRows(now.getTime(), lastPrice);
                                // Update current prices
                                Map<String, Price> prices = exchange.price(publicAPI);
                                String fileName = Utils.fromDate(Utils.FORMAT, now) + ".csv";
                                List<CsvRow> updatedRows = storage.updatedRowsAndSaveLastPrices(previousRows, prices,
                                        now, lastPrice, fearGreedIndex.getValue());
                                StringBuilder builder = new StringBuilder();
                                for (CsvRow row : updatedRows) {
                                    builder.append(row.toCsvLine());
                                }
                                String downloadLink = storage.updateFile("data" + exchange.getFolder() + fileName,
                                        builder.toString().getBytes(Utils.UTF8), CSV_HEADER_TOTAL);
                                // Notify bot
                                List<String> userIds = groupedFolder.get(exchange.getFolder());
                                if (userIds != null) {
                                    LOGGER.info("Sending bot messages to " + userIds + " for " + exchange.getFolder());
                                    publisher.publish(userIds.toArray(new String[0]));
                                }
                                response.getWriter().write("<" + downloadLink + ">");
                            } catch (Exception e) {
                                response.getWriter().write("<ERROR: " + exchange.name() + ". " + e.getMessage() + ">");
                                LOGGER.log(Level.SEVERE, "Cannot process " + exchange.name(), e);
                            }
                        }
                    } else {
                        LOGGER.log(Level.WARNING, exchange.name() + " is disabled");
                    }
                }
            } else {
                response.getWriter().write("Skipped");
            }
            client.close();
            response.setStatusCode(200);
        }
    }

    private void klines(Date now, FileManager storage, PublicAPI publicAPI, CloudProperties cloudProperties)
            throws FileNotFoundException, IOException {
        if (Utils.isTime(now, 0)) {
            String fileName = Utils.fromDate(Utils.FORMAT, now) + ".csv";
            Date yesterday = Utils.getDateOfDaysBackZero(now, 1);
            StringBuilder body = new StringBuilder();
            for (String symbol : cloudProperties.KLINES_LIST) {
                Kline kline = publicAPI.klines(Interval.DAY_1, symbol, 1, yesterday.getTime(), null).get(0);
                body.append(kline.toCsv());

            }
            storage.updateFile("data/klines/" + fileName, body.toString().getBytes(Utils.UTF8),
                    Utils.KLINE_ROW_HEADER.getBytes(Utils.UTF8));
        }
    }

}
