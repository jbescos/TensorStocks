package com.jbescos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.cloudbot.BotExecution;
import com.jbescos.cloudbot.BotUtils;
import com.jbescos.cloudchart.BarChart;
import com.jbescos.cloudchart.ChartGenerator;
import com.jbescos.cloudchart.DateChart;
import com.jbescos.cloudchart.IChart;
import com.jbescos.cloudchart.XYChart;
import com.jbescos.common.Broker;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;
import com.jbescos.test.util.TestFileStorage;

public class BotTest {

    private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());
    private static final CloudProperties CLOUD_PROPERTIES = DataLoader.CLOUD_PROPERTIES;
    private static final long HOURS_MILLIS = 3600 * 1000;
    private static final long DAY_MILLIS = HOURS_MILLIS * 24;
    private static final long MONTH_MILLIS = DAY_MILLIS * 30;
    private static final long HOURS_BACK_MILLIS = CLOUD_PROPERTIES.BOT_HOURS_BACK_STATISTICS * HOURS_MILLIS;
    private static final long TRANSACTIONS_BACK_MILLIS = CLOUD_PROPERTIES.BOT_MONTHS_BACK_TRANSACTIONS * MONTH_MILLIS;
    private static final List<TestResult> results = Collections.synchronizedList(new ArrayList<>());
    private static final int TOP = 40;
    private static final DataLoader LOADER = new DataLoader();
    
    @BeforeClass
    public static void beforeClass() throws IOException {
        LOADER.loadData("2021-05-20", null);
    }

    private void printResult() {
        Collections.sort(results, (a, b) -> Double.compare(b.multiplier, a.multiplier));
        double total = 0;
        double transactionsValue = 0;
        int transactions = 0;
        for (TestResult result : results) {
            total = total + result.multiplier;
            transactions = transactions + result.transactions;
            transactionsValue = transactionsValue + result.transactionValue;
        }
        LOGGER.info(() -> results.toString());
        double totalMultiplier = total / results.size();
        LOGGER.info(() -> "Total multiplier: " + totalMultiplier);
        LOGGER.info("Total transactions: " + transactions);
        LOGGER.info("Total multiplier per transaction: " + Utils.format(totalMultiplier / transactions));
        int top = results.size() < TOP ? results.size() : TOP;
        StringBuilder topInfo = new StringBuilder("TOP " + top + ":\nbot.white.list=");
        for (int i = 0; i < top; i++) {
            if (i != 0) {
                topInfo.append(",");
            }
            topInfo.append(results.get(i).symbol);
        }
        LOGGER.info(() -> topInfo.toString());
    }

	@Test
	@Ignore
    public void allTogether() throws IOException {
		final double INITIAL_USDT = 1000;
		Map<String, Double> wallet = new HashMap<>();
        wallet.put(Utils.USDT, INITIAL_USDT);
        List<CsvRow> walletHistorical = new ArrayList<>();
    	List<CsvTransactionRow> transactions = new ArrayList<>();
    	CsvRow first = LOADER.first();
    	long now = first.getDate().getTime() + HOURS_BACK_MILLIS;
    	long last = LOADER.last(first.getSymbol()).getDate().getTime();
    	while (now <= last) {
    		long previous = now - HOURS_BACK_MILLIS;
    		List<CsvRow> segment = LOADER.get(previous, now);
    		Date fromTx = new Date(now - TRANSACTIONS_BACK_MILLIS);
    		List<CsvTransactionRow> tx = transactions.stream().filter(row -> row.getDate().getTime() >= fromTx.getTime()).collect(Collectors.toList());
    	    List<Broker> stats = BotUtils.fromCsvRows(CLOUD_PROPERTIES, segment, tx);
    	    try {
    	    	BotExecution trader = BotExecution.test(CLOUD_PROPERTIES, new TestFileStorage("./target/total_"), wallet, transactions, walletHistorical, CLOUD_PROPERTIES.BINANCE_MIN_TRANSACTION);
                trader.execute(stats);
            } catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
    	    CsvRow lastRow = segment.get(segment.size() - 1);
    	    CsvRow next = LOADER.next(lastRow.getSymbol(), lastRow);
    	    if (next != null) {
    	        now = next.getDate().getTime();
    	    } else {
    	        break;
    	    }
    	}
    	List<CsvRow> totalWalletHistorical = walletHistorical.stream().filter(row -> row.getSymbol().startsWith("TOTAL")).collect(Collectors.toList());
    	double totalPrice = totalWalletHistorical.get(totalWalletHistorical.size() - 1).getPrice();
    	LOGGER.info(() -> "Wallet before: " + INITIAL_USDT + " " + Utils.USDT);
    	LOGGER.info(() -> "Wallet after: " + totalPrice + " " + Utils.USDT);
    	LOGGER.info(() -> ((totalPrice * 100) / INITIAL_USDT) + "%, " + Utils.format(totalPrice) + " " + Utils.USDT);
    	File chartFile = new File("./target/total.png");
        try (FileOutputStream output = new FileOutputStream(chartFile)) {
        	IChart<IRow> chart = new DateChart();
        	ChartGenerator.writeChart(walletHistorical, output, chart);
            ChartGenerator.save(output, chart);
        }
	}

    @Test
    public void separately() throws IOException {
        LOADER.symbols().parallelStream().forEach(symbol -> {
            CsvRow first = LOADER.first(symbol);
            Map<String, Double> wallet = new HashMap<>();
            wallet.put(Utils.USDT, first.getPrice());
            check(symbol, wallet);
        });
        printResult();
    }

    private void check(String symbol, Map<String, Double> wallet) {
    	double holderTotalUsd = wallet.get(Utils.USDT).doubleValue();
    	final double MIN_TX = holderTotalUsd * 0.1;
    	List<CsvRow> walletHistorical = new ArrayList<>();
    	List<CsvTransactionRow> transactions = new ArrayList<>();
    	CsvRow first = LOADER.first(symbol);
    	long now = first.getDate().getTime() + HOURS_BACK_MILLIS;
    	long last = LOADER.last(symbol).getDate().getTime();
    	LOGGER.info(() -> symbol + " loaded from " + Utils.fromDate(Utils.FORMAT_SECOND, first.getDate()) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, LOADER.last(symbol).getDate()));
    	while (now <= last) {
    	    long previous = now - HOURS_BACK_MILLIS;
    	    List<CsvRow> segment = LOADER.get(symbol, previous, now);
    	    Date fromTx = new Date(now - TRANSACTIONS_BACK_MILLIS);
    	    List<CsvTransactionRow> tx = transactions.stream().filter(row -> row.getDate().getTime() >= fromTx.getTime()).collect(Collectors.toList());
    	    List<Broker> stats = BotUtils.fromCsvRows(CLOUD_PROPERTIES, segment, tx);
    	    try {
    	    	BotExecution trader = BotExecution.test(CLOUD_PROPERTIES, new TestFileStorage("./target/" + symbol + "_"), wallet, transactions, walletHistorical, MIN_TX);
                trader.execute(stats);
            } catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
    	    CsvRow next = LOADER.next(symbol, segment.get(segment.size() - 1));
    	    if (next != null) {
    	        now = next.getDate().getTime();
    	    } else {
    	        break;
    	    }
    	}
        if (!transactions.isEmpty()) {
        	List<CsvRow> totalWalletHistorical = walletHistorical.stream().filter(row -> row.getSymbol().startsWith("TOTAL")).collect(Collectors.toList());
        	double usdSnapshot = totalWalletHistorical.isEmpty() ? 0 : totalWalletHistorical.get(totalWalletHistorical.size() - 1).getPrice();
        	TestResult result = new TestResult(first.getSymbol(), usdSnapshot, holderTotalUsd, transactions.size());
            results.add(result);
            chart(LOADER.get(symbol), result, wallet, transactions, totalWalletHistorical);
        }
    }

    private void chart(List<CsvRow> rows, TestResult result, Map<String, Double> wallet, List<CsvTransactionRow> transactions, List<CsvRow> walletHistorical) {
        CsvRow last = rows.get(rows.size() - 1);
        String subfix = result.success ? "_success" : "_failure";
        File chartFile = new File("./target/" + last.getSymbol() + subfix + ".png");
        try (FileOutputStream output = new FileOutputStream(chartFile)) {
            IChart<IRow> chart = new XYChart();
            ChartGenerator.writeChart(transactions, output, chart);
            ChartGenerator.writeChart(rows, output, chart);
            ChartGenerator.writeChart(walletHistorical, output, chart);
            ChartGenerator.save(output, chart);
        } catch (IOException e) {}
        File barChartFile = new File("./target/" + last.getSymbol() + subfix + "_bar.png");
        try (FileOutputStream output = new FileOutputStream(barChartFile)) {
            Map<String, Double> walletUsdt = new HashMap<>();
            for (Entry<String, Double> entry : wallet.entrySet()) {
                walletUsdt.put(entry.getKey(),  entry.getValue() * last.getPrice());
            }
            IChart<IRow> chart = new BarChart(walletUsdt);
            ChartGenerator.writeChart(transactions, output, chart);
            ChartGenerator.save(output, chart);
        } catch (IOException e) {}
    }

    @Test
    public void round() {
        assertEquals("21330.88888787", Utils.format(21330.888887878787));
    }

    @Test
    public void usdtPerUnit() {
        double quoteOrderQtyBD = Double.parseDouble("11.801812");
        double executedQtyBD = Double.parseDouble("0.47700000");
        double result = quoteOrderQtyBD / executedQtyBD;
        String resultStr = Utils.format(result);
        assertEquals("24.74174423", resultStr);
    }

    private static class TestResult {
        private final String symbol;
        private final double trader;
        private final double holder;
        private final double absoluteBenefit;
        private final double multiplier;
        private final int transactions;
        private final boolean success;
        private final double transactionValue;

        public TestResult(String symbol, double trader, double holder, int transactions) {
            this.symbol = symbol;
            this.trader = trader;
            this.holder = holder;
            this.absoluteBenefit = trader - holder;
            this.multiplier = trader / holder;
            this.transactions = transactions;
            this.success = absoluteBenefit >= 0;
            this.transactionValue = multiplier / transactions;
        }

        @Override
        public String toString() {
            return "\n TestResult [symbol=" + symbol + ", trader=" + trader + ", holder=" + holder + ", absoluteBenefit="
                    + absoluteBenefit + ", multiplier=" + multiplier + ", transactions=" + transactions + ", transactionValue=" + Utils.format(transactionValue) + ", success="
                    + success + "]";
        }

    }

}
