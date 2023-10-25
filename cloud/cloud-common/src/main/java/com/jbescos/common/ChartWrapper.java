package com.jbescos.common;

import java.io.IOException;
import java.util.List;

import com.jbescos.common.ChartGenerator.AccountChartCsv;
import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.common.ChartGenerator.SymbolChartCsv;
import com.jbescos.common.ChartGenerator.TxSummaryChartCsv;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Utils;

public class ChartWrapper {
    
    private final CloudProperties cloudProperties;
    private final FileManager storage;
    
    public ChartWrapper(CloudProperties cloudProperties, FileManager storage) {
        this.cloudProperties = cloudProperties;
        this.storage = storage;
    }

    public IChartCsv chartPng(String type,  List<String> symbols) throws IOException {
        IChartCsv chart = null;
        if (Utils.CHART_TYPE_LINE.equals(type)) {
            if (symbols == null || symbols.isEmpty()) {
                chart = new AccountChartCsv(storage, cloudProperties);
            } else {
                chart = new SymbolChartCsv(storage, cloudProperties, symbols);
            }
        } else if (Utils.CHART_TYPE_SUMMARY.equals(type)) {
            chart = new TxSummaryChartCsv(storage, cloudProperties, symbols);
        } else {
            throw new IllegalArgumentException("Unknown type=" + type);
        }
        return chart;
    }
}
