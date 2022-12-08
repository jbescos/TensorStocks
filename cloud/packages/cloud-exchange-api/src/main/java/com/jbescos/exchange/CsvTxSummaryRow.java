package com.jbescos.exchange;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CsvTxSummaryRow implements IRow {

    public static final byte[] CSV_HEADER_TX_SUMMARY_TOTAL = "DATE,SYMBOL,SUMMARY\r\n".getBytes(Utils.UTF8);
    private Date date;
    private String dateStr;
    private String symbol;
    private double summary;

    public CsvTxSummaryRow(Date date, String dateStr, String symbol, double summary) {
        this.date = date;
        this.dateStr = dateStr;
        this.symbol = symbol;
        this.summary = summary;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public double getPrice() {
        return summary;
    }

    @Override
    public String getLabel() {
        return symbol;
    }

    @Override
    public Double getAvg() {
        return null;
    }

    @Override
    public Double getAvg2() {
        return null;
    }

    public String toCsvLine() {
        StringBuilder builder = new StringBuilder();
        builder.append(dateStr).append(",").append(getLabel()).append(",").append(Utils.format(getPrice()))
                .append(Utils.NEW_LINE);
        return builder.toString();
    }

    public static List<CsvTxSummaryRow> fromMap(Date date, Map<String, Double> summaries) {
        List<CsvTxSummaryRow> list = new ArrayList<>();
        String dateStr = Utils.fromDate(Utils.FORMAT_SECOND, date);
        for (Entry<String, Double> entry : summaries.entrySet()) {
            CsvTxSummaryRow row = new CsvTxSummaryRow(date, dateStr, entry.getKey(), entry.getValue());
            list.add(row);
        }
        return list;
    }

    public static String toCsvBody(Date date, Map<String, Double> summaries) {
        StringBuilder builder = new StringBuilder();
        List<CsvTxSummaryRow> txRows = fromMap(date, summaries);
        for (CsvTxSummaryRow row : txRows) {
            builder.append(row.toCsvLine());
        }
        return builder.toString();
    }

}
