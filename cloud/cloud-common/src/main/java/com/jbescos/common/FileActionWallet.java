package com.jbescos.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;

import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Utils;

public class FileActionWallet implements FileAction {

    private final CloudProperties cloudProperties;
    private final Client client;
    private final FileManager storage;

    public FileActionWallet(CloudProperties cloudProperties, Client client, FileManager storage) {
        this.cloudProperties = cloudProperties;
        this.client = client;
        this.storage = storage;
    }

    @Override
    public void run() throws IOException {
        int days = 365;
        try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
            Map<String, Object> chartProperties = new HashMap<>();
            chartProperties.put(IChart.TITLE, "Wallet last " + days + " days");
            chartProperties.put(IChart.WIDTH, 1024);
            chartProperties.put(IChart.HEIGTH, 768);
            ChartWrapper chartWrapper = new ChartWrapper(cloudProperties, storage);
            IChartCsv chart = chartWrapper.chartPng(Utils.CHART_TYPE_LINE, Collections.emptyList());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartGenerator.writeLoadAndWriteChart(out, days, chart, chartProperties);
            telegram.sendImage(out.toByteArray());
        }
    }
    
}
