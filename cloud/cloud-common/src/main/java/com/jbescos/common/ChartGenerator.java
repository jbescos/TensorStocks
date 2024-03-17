package com.jbescos.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvAccountRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.CsvTxSummaryRow;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.IRow;
import com.jbescos.exchange.Utils;

public class ChartGenerator {

    private static final Logger LOGGER = Logger.getLogger(ChartGenerator.class.getName());
    private static final String DATA_PREFIX = "data";
    private static final int PRECISSION_CHART_DAYS = 7;

    public static void writeLoadAndWriteChart(OutputStream output, int daysBack, IChartCsv chartCsv, Map<String, Object> chartProperties)
            throws IOException {
        IChart<IRow> chart = chartCsv.chart(daysBack);
        for (Entry<String, Object> entry : chartProperties.entrySet()) {
            chart.property(entry.getKey(), entry.getValue());
        }
        List<IRow> rows = chartCsv.read(daysBack);
        writeChart(rows, output, chart);
        save(output, chart);
    }

    public static void writeChart(List<? extends IRow> rows, OutputStream output, IChart<IRow> chart)
            throws IOException {
        Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getLabel));
        List<String> keys = new ArrayList<>(grouped.keySet());
        Utils.sortForChart(keys);
        LOGGER.info(() -> "Chart display order " + keys);
        for (String key : keys) {
            chart.add(key, grouped.get(key));
        }
    }

    public static void save(OutputStream output, IChart<?> chart) throws IOException {
        chart.save(output);
    }

    public static interface IChartCsv {

        List<IRow> read(int daysBack) throws IOException;

        IChart<IRow> chart(int daysBack);
    }

    public static class AccountChartCsv implements IChartCsv {

        private final CloudProperties cloudProperties;
        private final FileManager storage;

        public AccountChartCsv(FileManager storage, CloudProperties cloudProperties) {
            this.cloudProperties = cloudProperties;
            this.storage = storage;
        }

        @Override
        public List<IRow> read(int daysBack) throws IOException {
            Date now = new Date();
            Date from = Utils.getDateOfDaysBack(now, daysBack);
            List<String> months = Utils.monthsBack(now, (daysBack / 31) + 2,
                    cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX, ".csv");
            List<IRow> rows = new ArrayList<>();
            for (String month : months) {
                String raw = storage.getRaw(month);
                if (raw != null) {
                    BufferedReader reader = new BufferedReader(new StringReader(raw));
                    List<CsvAccountRow> rowsInMonth = CsvUtil.readCsvAccountRows(true, ",", reader).stream()
                            .filter(row -> row.getDate().getTime() >= from.getTime())
                            .filter(row -> row.getPrice() > Utils.MIN_WALLET_VALUE_TO_RECORD)
                            .collect(Collectors.toList());
                    rows.addAll(rowsInMonth);
                }
            }
            return rows;
        }

        @Override
        public IChart<IRow> chart(int daysBack) {
            if (daysBack > PRECISSION_CHART_DAYS) {
                return new DateChart();
            } else {
                return new XYChart();
            }
        }

    }

    public static class TxSummaryChartCsv implements IChartCsv {

        private final CloudProperties cloudProperties;
        private final FileManager storage;
        private final List<String> symbols;

        public TxSummaryChartCsv(FileManager storage, CloudProperties cloudProperties, List<String> symbols) {
            this.storage = storage;
            this.cloudProperties = cloudProperties;
            this.symbols = symbols == null ? Collections.emptyList() : symbols;
        }

        @Override
        public List<IRow> read(int daysBack) throws IOException {
            Date now = new Date();
            List<IRow> total = new ArrayList<>();
            List<String> days = Utils.daysBack(now, daysBack, cloudProperties.USER_ID + "/" + Utils.TX_SUMMARY_PREFIX,
                    ".csv");
            for (String day : days) {
                String raw = storage.getRaw(day);
                if (raw != null) {
                    BufferedReader reader = new BufferedReader(new StringReader(raw));
                    List<? extends IRow> rows = CsvUtil.readCsvTxSummaryRows(true, ",", reader).stream()
                            .filter(row -> {
                                if (!symbols.isEmpty() && !symbols.contains(row.getLabel())) {
                                    return false;
                                } else {
                                    return true;
                                }
                            }).collect(Collectors.toList());
                    if (daysBack > PRECISSION_CHART_DAYS) {
                        // Pick the last to avoid memory issues
                        Map<String, List<IRow>> grouped = rows.stream()
                                .collect(Collectors.groupingBy(IRow::getLabel));
                        List<IRow> lastOfEachSymbol = new ArrayList<>();
                        for (List<IRow> values : grouped.values()) {
                            if (!values.isEmpty()) {
                                lastOfEachSymbol.add(values.get(values.size() - 1));
                            }
                        }
                        total.addAll(lastOfEachSymbol);
                    } else {
                        total.addAll(rows);
                    }
                }
            }
            Optional<IRow> min = total.stream().min((r1, r2) -> r1.getDate().compareTo(r2.getDate()));
            if (min.isPresent()) {
                // Set a line in the zero
                final String ZERO = "ZERO";
                CsvTxSummaryRow zero0 = new CsvTxSummaryRow(min.get().getDate(),
                        Utils.fromDate(Utils.FORMAT_SECOND, min.get().getDate()), ZERO, 0);
                CsvTxSummaryRow zero1 = new CsvTxSummaryRow(now, Utils.fromDate(Utils.FORMAT_SECOND, now), ZERO, 0);
                total.add(zero0);
                total.add(zero1);
            }
            return total;
        }

        @Override
        public IChart<IRow> chart(int daysBack) {
            if (daysBack > PRECISSION_CHART_DAYS) {
                return new DateChart();
            } else {
                return new XYChart();
            }
        }

    }

    public static class SymbolChartCsv implements IChartCsv {

        private final List<String> symbols;
        private final FileManager storage;
        private final CloudProperties cloudProperties;

        public SymbolChartCsv(FileManager storage, CloudProperties cloudProperties, List<String> symbols) {
            this.storage = storage;
            this.cloudProperties = cloudProperties;
            this.symbols = symbols;
        }

        @Override
        public IChart<IRow> chart(int daysBack) {
            if (daysBack > PRECISSION_CHART_DAYS) {
                return new DateChart();
            } else {
                return new XYChart();
            }
        }

        @Override
        public List<IRow> read(int daysBack) throws IOException {
            Date now = new Date();
            List<IRow> total = new ArrayList<>();
            List<String> days = Utils.daysBack(now, daysBack, DATA_PREFIX + cloudProperties.USER_EXCHANGE.getFolder(),
                    ".csv");
            for (String day : days) {
                String raw = storage.getRaw(day);
                if (raw != null) {
                    BufferedReader reader = new BufferedReader(new StringReader(raw));
                    List<? extends IRow> rows = CsvUtil.readCsvRows(true, ",", reader, symbols);
                    if (daysBack > PRECISSION_CHART_DAYS) {
                        // Pick the last to avoid memory issues
                        Map<String, List<IRow>> grouped = rows.stream()
                                .collect(Collectors.groupingBy(IRow::getLabel));
                        List<IRow> lastOfEachSymbol = new ArrayList<>();
                        for (List<IRow> values : grouped.values()) {
                            if (!values.isEmpty()) {
                                lastOfEachSymbol.add(values.get(values.size() - 1));
                            }
                        }
                        total.addAll(lastOfEachSymbol);
                    } else {
                        total.addAll(rows);
                    }
                }
            }
            Date from = Utils.getDateOfDaysBack(now, daysBack);
            List<String> months = Utils.monthsBack(now, (daysBack / 31) + 2,
                    cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
            LOGGER.info(() -> "Loading transactions of " + months);
            List<CsvTransactionRow> totalTransactions = new ArrayList<>();
            for (String month : months) {
                String raw = storage.getRaw(month);
                if (raw != null) {
                    BufferedReader reader = new BufferedReader(new StringReader(raw));
                    List<CsvTransactionRow> transactions = CsvUtil.readCsvTransactionRows(true, ",", reader)
                            .stream().filter(row -> row.getDate().getTime() >= from.getTime())
                            .collect(Collectors.toList());
                    if (symbols != null && !symbols.isEmpty()) {
                        transactions = transactions.stream().filter(row -> symbols.contains(row.getSymbol()))
                                .collect(Collectors.toList());
                    }
                    transactions.stream().forEach(tx -> tx.setUsdt(tx.getUsdtUnit()));
                    totalTransactions.addAll(transactions);
                    total.addAll(transactions);
                }
            }
            total.addAll(profitBarriers(totalTransactions));
            return total;
        }

        private List<IRow> profitBarriers(List<CsvTransactionRow> totalTransactions) {
            List<IRow> barriers = new ArrayList<>();
            double totalPriceBuy = 0;
            int nPurchases = 0;
            Date date = null;
            for (CsvTransactionRow tx : totalTransactions) {
                if (tx.getSide() == Action.SELL || tx.getSide() == Action.SELL_PANIC) {
                    if (nPurchases != 0) {
                        double avgPurchase = totalPriceBuy / nPurchases;
                        barriers.add(barrierPoint(date, avgPurchase));
                        barriers.add(barrierPoint(tx.getDate(), avgPurchase));
                        totalPriceBuy = 0;
                        nPurchases = 0;
                    }
                } else if (tx.getSide() == Action.BUY) {
                    if (nPurchases == 0) {
                        date = tx.getDate();
                    }
                    totalPriceBuy = totalPriceBuy + tx.getPrice();
                    nPurchases++;
                }
            }
            return barriers;
        }

        private IRow barrierPoint(Date date, double avgPurchase) {
            String profitBarrierName = "PROFIT_BARRIER";
            return new IRow() {
                @Override
                public double getPrice() {
                    return avgPurchase;
                }
                @Override
                public String getLabel() {
                    return profitBarrierName;
                }
                @Override
                public Date getDate() {
                    return date;
                }
                @Override
                public Double getAvg2() {
                    return null;
                }
                @Override
                public Double getAvg() {
                    return null;
                }
            };
        }
    }
    
}
