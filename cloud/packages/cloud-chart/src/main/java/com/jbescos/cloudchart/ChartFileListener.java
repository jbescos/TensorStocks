package com.jbescos.cloudchart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.jbescos.cloudchart.ChartFileListener.GCSEvent;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.ChartGenerator;
import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.StorageInfo;
import com.jbescos.common.TelegramBot;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.Utils;

// Entry: com.jbescos.cloudchart.ChartFileListener
public class ChartFileListener implements BackgroundFunction<GCSEvent> {

    private static final Logger LOGGER = Logger.getLogger(ChartFileListener.class.getName());
    private static final long MINUTES_5 = 1000 * 60 * 5;

    // Finalize or create
    @Override
    public void accept(GCSEvent event, Context context) {
        LOGGER.info("Processing file: " + event.name);
        try {
            // test-Kucoin/profit/profit_2023-03.csv
            String[] folders = event.name.split("/");
            if (folders.length == 3) {
                String userId = folders[0];
                String folder = folders[1];
                String file = folders[2];
                if ("profit".equals(folder)) {
                    Client client = ClientBuilder.newClient();
                    StorageInfo storageInfo = StorageInfo.build();
                    CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
                    BucketStorage bucketStorage = new BucketStorage(storageInfo);
                    List<CsvProfitRow> profit = bucketStorage.loadCsvProfitRows(event.name);
                    List<CsvProfitRow> recentProfits = Utils.lastModified(profit, r -> r.getSellDate());
                    ChartWrapper chartWrapper = new ChartWrapper(cloudProperties);
                    Date now = new Date();
                    try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
                        for (CsvProfitRow recentProfit : recentProfits) {
                            long difference = now.getTime() - recentProfit.getSellDate().getTime();
                            // Avoid to send image again when sometimes it is synced
                            if (difference < MINUTES_5 && !recentProfit.isSync()) {
                                IChartCsv chart = chartWrapper.chartPng(ChartFunction.TYPE_LINE, Arrays.asList(recentProfit.getSymbol()));
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                ChartGenerator.writeLoadAndWriteChart(out, 7, chart);
                                telegram.sendImage(out.toByteArray());
                            }
                        }
                    }
                    client.close();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot work with " + event.name, e);
        }
    }

    public static class GCSEvent {
        String bucket;
        String name;
        String metageneration;
    }
}
