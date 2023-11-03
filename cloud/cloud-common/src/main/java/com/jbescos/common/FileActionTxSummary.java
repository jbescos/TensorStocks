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

public class FileActionTxSummary implements FileAction {

    private final CloudProperties cloudProperties;
    private final Client client;
    private final FileManager storage;

    public FileActionTxSummary(CloudProperties cloudProperties, Client client, FileManager storage) {
        this.cloudProperties = cloudProperties;
        this.client = client;
        this.storage = storage;
    }

    @Override
    public void run() throws IOException {
        ChartWrapper chartWrapper = new ChartWrapper(cloudProperties, storage);
        IChartCsv chart = chartWrapper.chartPng(Utils.CHART_TYPE_SUMMARY, Collections.emptyList());
        try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
            Map<String, Object> chartProperties = new HashMap<>();
            chartProperties.put(IChart.TITLE, "Summary of profits during the week");
            chartProperties.put(IChart.VERTICAL_LABEL, "Profit per 1");
            chartProperties.put(IChart.WIDTH, 1024);
            chartProperties.put(IChart.HEIGTH, 768);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartGenerator.writeLoadAndWriteChart(out, 7, chart, chartProperties);
            telegram.sendImage(out.toByteArray());
        }
    }

}
