package com.jbescos.cloudchart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.jbescos.common.IChart;
import com.jbescos.common.StorageInfo;
import com.jbescos.common.TelegramBot;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.FileManager;
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
            // test-Kucoin/wallet/wallet_2023-03.csv
            new ActionExecutor().run(event.name);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot process " + event.name, e);
        }
    }

    private static interface Action {
        void run() throws IOException;
    }
    
    private static class ProfitAction implements Action {
        private final CloudProperties cloudProperties;
        private final Client client;
        private final FileManager storage;
        private final String fileName;
        public ProfitAction(CloudProperties cloudProperties, Client client, FileManager storage, String fileName) {
            this.cloudProperties = cloudProperties;
            this.client = client;
            this.storage = storage;
            this.fileName = fileName;
        }
        @Override
        public void run() throws IOException {
            List<CsvProfitRow> profit = storage.loadCsvProfitRows(fileName);
            ChartWrapper chartWrapper = new ChartWrapper(cloudProperties);
            Date now = new Date();
            int days = 7;
            try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
                for (int i = profit.size() - 1; i >=0; i--) {
                    CsvProfitRow recentProfit = profit.get(i);
                    long difference = now.getTime() - recentProfit.getSellDate().getTime();
                    // Avoid to send image again when sometimes it is synced
                    if (difference < MINUTES_5 && !recentProfit.isSync()) {
                        Map<String, Object> chartProperties = new HashMap<>();
                        chartProperties.put(IChart.TITLE, "Last " + days + " days of " + recentProfit.getSymbol());
                        chartProperties.put(IChart.WIDTH, 1024);
                        chartProperties.put(IChart.HEIGTH, 768);
                        IChartCsv chart = chartWrapper.chartPng(ChartFunction.TYPE_LINE, Arrays.asList(recentProfit.getSymbol()));
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ChartGenerator.writeLoadAndWriteChart(out, days, chart, chartProperties);
                        telegram.sendImage(out.toByteArray());
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private static class WalletAction implements Action {
        private final CloudProperties cloudProperties;
        private final Client client;
        public WalletAction(CloudProperties cloudProperties, Client client) {
            this.cloudProperties = cloudProperties;
            this.client = client;
        }
        @Override
        public void run() throws IOException {
            int days = 365;
            try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
                Map<String, Object> chartProperties = new HashMap<>();
                chartProperties.put(IChart.TITLE, "Wallet last " + days + " days");
                chartProperties.put(IChart.WIDTH, 1024);
                chartProperties.put(IChart.HEIGTH, 768);
                ChartWrapper chartWrapper = new ChartWrapper(cloudProperties);
                IChartCsv chart = chartWrapper.chartPng(ChartFunction.TYPE_LINE, Collections.emptyList());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ChartGenerator.writeLoadAndWriteChart(out, days, chart, chartProperties);
                telegram.sendImage(out.toByteArray());
            }
        }
        
    }

    private static class ActionExecutor {
        
        public void run(String name) throws IOException {
            String[] folders = name.split("/");
            if (folders.length == 3) {
                String userId = folders[0];
                String folder = folders[1];
                String file = folders[2];
                Action action = null;
                Client client = null;
                try {
                    if ("profit".equals(folder)) {
                        client = ClientBuilder.newClient();
                        StorageInfo storageInfo = StorageInfo.build();
                        FileManager storage = new BucketStorage(storageInfo);
                        CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
                        action = new ProfitAction(cloudProperties, client, storage, name);
                    } else if ("wallet".equals(folder)) {
                        if (isReportTime(new Date())) {
                            client = ClientBuilder.newClient();
                            StorageInfo storageInfo = StorageInfo.build();
                            CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
                            action = new WalletAction(cloudProperties, client);
                        }
                    }
                    if (action != null) {
                        action.run();
                    }
                } finally {
                    if (client != null) {
                        client.close();
                    }
                }
                
            }
        }

        // Reports if the bot run between 6:00 and 6:10
        private boolean isReportTime(Date now) {
            return Utils.isTime(now, Utils.REPORT_HOUR);
        }
    }
    
    public static class GCSEvent {
        String bucket;
        String name;
        String metageneration;
    }
}
