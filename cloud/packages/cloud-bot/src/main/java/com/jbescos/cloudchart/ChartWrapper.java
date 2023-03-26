package com.jbescos.cloudchart;

import java.io.IOException;
import java.util.List;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.ChartGenerator.AccountChartCsv;
import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.common.ChartGenerator.ProfitableBarChartCsv;
import com.jbescos.common.ChartGenerator.SymbolChartCsv;
import com.jbescos.common.ChartGenerator.TxSummaryChartCsv;

public class ChartWrapper {
    
    private final CloudProperties cloudProperties;
    
    public ChartWrapper(CloudProperties cloudProperties) {
        this.cloudProperties = cloudProperties;
    }

    public IChartCsv chartPng(String type,  List<String> symbols) throws IOException {
        IChartCsv chart = null;
        if (ChartFunction.TYPE_LINE.equals(type)) {
            if (symbols == null || symbols.isEmpty()) {
                chart = new AccountChartCsv(cloudProperties);
            } else {
                chart = new SymbolChartCsv(cloudProperties, symbols);
            }
        } else if (ChartFunction.TYPE_BAR.equals(type)) {
            chart = new ProfitableBarChartCsv(cloudProperties, symbols);
        } else if (ChartFunction.TYPE_SUMMARY.equals(type)) {
            chart = new TxSummaryChartCsv(cloudProperties, symbols);
        } else {
            throw new IllegalArgumentException("Unknown type=" + type);
        }
        return chart;
    }
}
