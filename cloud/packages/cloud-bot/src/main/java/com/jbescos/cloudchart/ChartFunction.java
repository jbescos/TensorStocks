package com.jbescos.cloudchart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.ChartGenerator;
import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.IChart;
import com.jbescos.common.StorageInfo;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.CsvTxSummaryRow;
import com.jbescos.exchange.Utils;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {

    private static final String HTML_PAGE = 
            "<html>" 
            + "<body>"
            + "<h1><USER_ID> Report</h1>"
            + " <img src=\"https://alternative.me/crypto/fear-and-greed-index.png\" alt=\"image\"/>"
            + "<h2>Summary of benefits</h2>"
            + " <img src=\"<CHART_URL>?userId=<USER_ID>&type=summary&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/>"
            + "<h2>Wallet 7 days</h2>"
            + " <img src=\"<CHART_URL>?userId=<USER_ID>&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/>"
            + "<h2>Wallet 365 days</h2>"
            + " <img src=\"<CHART_URL>?userId=<USER_ID>&days=365&uncache=<TIMESTAMP>\" alt=\"image\"/>"
            + "<h2>Open positions</h2><OPEN_POSITIONS>"
            + "</body>"
            + "</html>";
    private static final String USER_ID_PARAM = "userId";
    static final String TYPE_LINE = "line";
    static final String TYPE_BAR = "bar";
    static final String TYPE_SUMMARY = "summary";
    static final String TYPE_HTML = "html";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String userId = Utils.getParam(USER_ID_PARAM, null, request.getQueryParameters());
        if (userId == null || userId.isEmpty()) {       
            response.getWriter().write("Parameter userId is mandatory");
            response.setStatusCode(200);
            response.setContentType("text/plain");
        } else {
            StorageInfo storageInfo = StorageInfo.build();
            CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
            ChartWrapper wrapper = new ChartWrapper(cloudProperties);
            String type = Utils.getParam("type", TYPE_LINE, request.getQueryParameters());
            if (TYPE_HTML.equals(type)) {
                response.setContentType("text/html");
                BucketStorage bucketStorage = new BucketStorage(storageInfo);
                Map<String, List<CsvTransactionRow>> txPerSymbol = bucketStorage.loadOpenTransactions(userId).stream().collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
                StringBuilder openTxLinks = new StringBuilder();
                for (Entry<String, List<CsvTransactionRow>> txs : txPerSymbol.entrySet()) {
                    String h3 = "<h3><SYMBOL></h3>";
                    String img = "<p><img src=\"<CHART_URL>?userId=<USER_ID>&symbol=<SYMBOL>&days=7&uncache=<TIMESTAMP>\" alt=\"<CHART_URL>?userId=<USER_ID>&symbol=<SYMBOL>&days=7&uncache=<TIMESTAMP>\"/></p>";
                    StringBuilder openTxTable = new StringBuilder().append("<table border=\"1\"><tr><th>Index</th><th>Date</th><th>USD/unit</th><th>USD</th></tr>");
                    int idx = 1;
                    for (CsvTransactionRow tx : txs.getValue()) {
                        openTxTable.append("<tr>");
                        openTxTable.append("<td>").append(idx).append("</td>");
                        openTxTable.append("<td>").append(Utils.fromDate(Utils.FORMAT_SECOND, tx.getDate())).append("</td>");
                        openTxTable.append("<td>").append(Utils.format(tx.getUsdtUnit())).append("</td>");
                        openTxTable.append("<td>").append(tx.getUsdt()).append("</td>");
                        openTxTable.append("</tr>");
                        idx++;
                    }
                    openTxTable.append("</table>");
                    openTxLinks.append(h3.replaceFirst("<SYMBOL>", txs.getKey()));
                    openTxLinks.append(openTxTable);
                    openTxLinks.append(img.replaceFirst("<SYMBOL>", txs.getKey()));
                }
                String htmlPage = HTML_PAGE
                        .replaceAll("<OPEN_POSITIONS>", openTxLinks.toString())
                        .replaceAll("<USER_ID>", userId).replaceAll("<CHART_URL>", cloudProperties.CHART_URL)
                        .replaceAll("<TIMESTAMP>", Long.toString(System.currentTimeMillis()));
                response.getWriter().append(htmlPage);
                response.getWriter().flush();
            } else {
                int daysBack = Integer.parseInt(Utils.getParam("days", "365", request.getQueryParameters()));
                List<String> symbols = request.getQueryParameters().get("symbol");
                IChartCsv chart = wrapper.chartPng(type, symbols);
                String fileName = chart.getClass().getSimpleName() + "_" + Utils.today() + ".png";
                response.setContentType("image/png");
                response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                Map<String, Object> chartProperties = new HashMap<>();
                if (TYPE_SUMMARY.equals(type)) {
                    chartProperties.put(IChart.TITLE, "Summary of profitability");
                    chartProperties.put(IChart.VERTICAL_LABEL, "Profitability per 1");
                } else if (TYPE_LINE.equals(type)) {
                    if (symbols != null && symbols.size() == 1) {
                        chartProperties.put(IChart.TITLE, "Last " + daysBack + " days of " + symbols.get(0));
                    } else if (symbols == null || symbols.isEmpty()) {
                        chartProperties.put(IChart.TITLE, "Wallet last " + daysBack + " days");
                    }
                }
                ChartGenerator.writeLoadAndWriteChart(response.getOutputStream(), daysBack, chart, chartProperties);
                response.getOutputStream().flush();
            }
        }
    }

}
