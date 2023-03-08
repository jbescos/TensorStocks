package com.jbescos.cloudchart;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.ChartGenerator;
import com.jbescos.common.ChartGenerator.AccountChartCsv;
import com.jbescos.common.ChartGenerator.IChartCsv;
import com.jbescos.common.ChartGenerator.ProfitableBarChartCsv;
import com.jbescos.common.ChartGenerator.SymbolChartCsv;
import com.jbescos.common.ChartGenerator.TxSummaryChartCsv;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.StorageInfo;
import com.jbescos.exchange.Utils;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {

    private static final String HTML_PAGE = "<html>" + "<body>" + "<h1><USER_ID> Report</h1>"
            + " <img src=\"https://alternative.me/crypto/fear-and-greed-index.png\" alt=\"image\"/>"
            + "<h2>Summary of benefits</h2>"
            + " <img src=\"<CHART_URL>?userId=<USER_ID>&type=summary&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/>"
            + "<h2>Wallet 7 days</h2>"
            + " <img src=\"<CHART_URL>?userId=<USER_ID>&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/>"
            + "<h2>Wallet 365 days</h2>"
            + " <img src=\"<CHART_URL>?userId=<USER_ID>&days=365&uncache=<TIMESTAMP>\" alt=\"image\"/>"
            + "<h2>Open positions</h2><OPEN_POSITIONS>" + "</body>" + "</html>";
    private static final String USER_ID_PARAM = "userId";
    private static final String TYPE_LINE = "line";
    private static final String TYPE_BAR = "bar";
    private static final String TYPE_SUMMARY = "summary";
    private static final String TYPE_HTML = "html";

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
            String type = Utils.getParam("type", TYPE_LINE, request.getQueryParameters());
            if (TYPE_HTML.equals(type)) {
                response.setContentType("text/html");
                BucketStorage bucketStorage = new BucketStorage(storageInfo);
                Set<String> symbols = bucketStorage.loadOpenTransactions(userId).stream().map(tx -> tx.getSymbol())
                        .collect(Collectors.toSet());
                StringBuilder openTxLinks = new StringBuilder();
                final String img = "<p><img src=\"<CHART_URL>?userId=<USER_ID>&symbol=<SYMBOL>&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/></p>";
                symbols.stream().forEach(symbol -> {
                    openTxLinks.append(img.replaceFirst("<SYMBOL>", symbol));
                });
                String htmlPage = HTML_PAGE.replaceAll("<OPEN_POSITIONS>", openTxLinks.toString())
                        .replaceAll("<USER_ID>", userId).replaceAll("<CHART_URL>", cloudProperties.CHART_URL)
                        .replaceAll("<TIMESTAMP>", Long.toString(System.currentTimeMillis()));
                response.getWriter().append(htmlPage);
                response.getWriter().flush();
            } else {
                String daysBack = Utils.getParam("days", "365", request.getQueryParameters());
                List<String> symbols = request.getQueryParameters().get("symbol");
                IChartCsv chart = null;
                if (TYPE_LINE.equals(type)) {
                    if (symbols == null || symbols.isEmpty()) {
                        chart = new AccountChartCsv(cloudProperties);
                    } else {
                        chart = new SymbolChartCsv(cloudProperties, symbols);
                    }
                } else if (TYPE_BAR.equals(type)) {
                    chart = new ProfitableBarChartCsv(cloudProperties, symbols);
                } else if (TYPE_SUMMARY.equals(type)) {
                    chart = new TxSummaryChartCsv(cloudProperties, symbols);
                } else {
                    throw new IllegalArgumentException("Unknown type=" + type);
                }
                String fileName = chart.getClass().getSimpleName() + "_" + Utils.today() + ".png";
                response.setContentType("image/png");
                response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                ChartGenerator.writeLoadAndWriteChart(response.getOutputStream(), Integer.parseInt(daysBack), chart);
                response.getOutputStream().flush();
            }
        }
    }

}
