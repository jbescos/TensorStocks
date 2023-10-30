package com.jbescos.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;

import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Utils;

public class FileActionProfit implements FileAction {

    private static final long MINUTES_5 = 1000 * 60 * 5;
    private final CloudProperties cloudProperties;
    private final Client client;
    private final FileManager storage;
    private final String fileName;

    public FileActionProfit(CloudProperties cloudProperties, Client client, FileManager storage, String fileName) {
        this.cloudProperties = cloudProperties;
        this.client = client;
        this.storage = storage;
        this.fileName = fileName;
    }

    @Override
    public void run() throws IOException {
        List<CsvProfitRow> profit = storage.loadCsvProfitRows(fileName);
        ChartWrapper chartWrapper = new ChartWrapper(cloudProperties, storage);
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
                    IChartCsv chart = chartWrapper.chartPng(Utils.CHART_TYPE_LINE, Arrays.asList(recentProfit.getSymbol()));
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