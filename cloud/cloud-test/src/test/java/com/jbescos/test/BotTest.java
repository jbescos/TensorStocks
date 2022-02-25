package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.Utils;
import com.jbescos.test.Simulation.Result;
import com.jbescos.test.util.TestFileStorage;

public class BotTest {
   
    private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());
    private static final List<TestResult> results = Collections.synchronizedList(new ArrayList<>());
    private static final int TOP = 400;

//    private void printResult() {
//        Collections.sort(results, (a, b) -> Double.compare(b.multiplier, a.multiplier));
//        double total = 0;
//        double transactionsValue = 0;
//        int transactions = 0;
//        for (TestResult result : results) {
//            total = total + result.multiplier;
//            transactions = transactions + result.transactions;
//            transactionsValue = transactionsValue + result.transactionValue;
//        }
//        LOGGER.info(() -> results.toString());
//        double totalMultiplier = total / results.size();
//        LOGGER.info(() -> "Total multiplier: " + totalMultiplier);
//        LOGGER.info("Total transactions: " + transactions);
//        LOGGER.info("Total multiplier per transaction: " + Utils.format(totalMultiplier / transactions));
//        int top = results.size() < TOP ? results.size() : TOP;
//        StringBuilder topInfo = new StringBuilder("TOP " + top + ":\nbot.white.list=");
//        for (int i = 0; i < top; i++) {
//            if (i != 0) {
//                topInfo.append(",");
//            }
//            topInfo.append(results.get(i).symbol);
//        }
//        LOGGER.info(() -> topInfo.toString());
//    }

    @Test
    public void allTogether() throws FileNotFoundException, IOException {
    	final String BASE_TEST_FOLDER = "./target/test-results/";
    	DateFormat format = new SimpleDateFormat(Utils.FORMAT);
    	List<Result> results = Collections.synchronizedList(new ArrayList<>());
    	List<CompletableFuture<?>> completables = new ArrayList<>();
    	final int MONTHS_INTERVAL = 1;
    	String[] users = new String [] {
    			"kucoin", "2021-10-25",
    			"binance", "2021-05-08",
    			"kucoin-all", "2021-10-25",
    			"binance-all", "2021-05-08",
    			"ftx", "2021-11-09",
    			"okex", "2021-11-09"
    			};
    	for (int i = 0; i < users.length; i = i + 2) {
    		String userId = users[i];
    		CloudProperties cloudProperties = new CloudProperties(userId);
    		String from = users[i + 1];
    		Date to = Utils.getStartOfSpecifiedMonth(Utils.fromString(format, from), MONTHS_INTERVAL);
    		String toStr = Utils.fromDate(Utils.FORMAT, to);
    		Simulation simulation = null;
    		while ((simulation = Simulation.build(from, toStr, BASE_TEST_FOLDER, cloudProperties)) != null) {
    		    final Simulation sim = simulation;
    		    CompletableFuture<Void> completable = CompletableFuture.supplyAsync(() -> sim.runTogether()).thenAccept(result -> results.add(result));
    		    completables.add(completable);
    			from = toStr;
    			to = Utils.getStartOfSpecifiedMonth(Utils.fromString(format, from), MONTHS_INTERVAL);
    			toStr = Utils.fromDate(Utils.FORMAT, to);
    		}
    	}
    	CompletableFuture.allOf(completables.toArray(new CompletableFuture[0])).join();
    	Collections.sort(results);
    	TestFileStorage fileStorage = new TestFileStorage(BASE_TEST_FOLDER, null, null);
    	StringBuilder builder = new StringBuilder();
    	results.forEach(result -> builder.append(result.toCsv()));
    	double avg = results.stream().mapToDouble(Result::getBenefitPercentage).average().getAsDouble();
    	builder.append("AVG,").append(Utils.format(avg)).append("%");
    	fileStorage.updateFile("results.csv", builder.toString().getBytes(), Result.CSV_HEAD.getBytes());
    	LOGGER.info("AVG: " + Utils.format(avg) + "%");
    }
    
//    @Test
//    @Ignore
//    public void separately() throws IOException {
//        LOADER.symbols().parallelStream().forEach(symbol -> {
//            CsvRow first = LOADER.first(symbol);
//            Map<String, Double> wallet = new HashMap<>();
//            wallet.put(Utils.USDT, first.getPrice());
//            check(symbol, wallet);
//        });
//        printResult();
//    }
//
//    private void check(String symbol, Map<String, Double> wallet) {
//    	double holderTotalUsd = wallet.get(Utils.USDT).doubleValue();
//    	final double MIN_TX = holderTotalUsd * 0.1;
//    	List<CsvRow> walletHistorical = new ArrayList<>();
//    	List<CsvTransactionRow> transactions = new ArrayList<>();
//    	CsvRow first = LOADER.first(symbol);
//    	long now = first.getDate().getTime() + HOURS_BACK_MILLIS;
//    	long last = LOADER.last(symbol).getDate().getTime();
//    	LOGGER.info(() -> symbol + " loaded from " + Utils.fromDate(Utils.FORMAT_SECOND, first.getDate()) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, LOADER.last(symbol).getDate()));
//    	while (now <= last) {
//    	    long previous = now - HOURS_BACK_MILLIS;
//    	    List<CsvRow> segment = LOADER.get(symbol, previous, now);
//    	    TestFileStorage fileManager = new TestFileStorage("./target/total_", transactions, segment);
//    	    try {
//    	    	List<Broker> stats = new DefaultBrokerManager(LOADER.getCloudProperties(), fileManager).loadBrokers();
//    	    	BotExecution trader = BotExecution.test(LOADER.getCloudProperties(), fileManager, wallet, transactions, walletHistorical, MIN_TX);
//                trader.execute(stats);
//            } catch (IOException e) {
//                e.printStackTrace();
//                fail(e.getMessage());
//            }
//    	    CsvRow next = LOADER.next(symbol, segment.get(segment.size() - 1));
//    	    if (next != null) {
//    	        now = next.getDate().getTime();
//    	    } else {
//    	        break;
//    	    }
//    	}
//        if (!transactions.isEmpty()) {
//        	List<CsvRow> totalWalletHistorical = walletHistorical.stream().filter(row -> row.getSymbol().startsWith("TOTAL")).collect(Collectors.toList());
//        	double usdSnapshot = totalWalletHistorical.isEmpty() ? 0 : totalWalletHistorical.get(totalWalletHistorical.size() - 1).getPrice();
//        	TestResult result = new TestResult(first.getSymbol(), usdSnapshot, holderTotalUsd, transactions.size());
//            results.add(result);
//            chart(LOADER.get(symbol), result, wallet, transactions, totalWalletHistorical);
//        }
//    }
//
//    private void chart(List<CsvRow> rows, TestResult result, Map<String, Double> wallet, List<CsvTransactionRow> transactions, List<CsvRow> walletHistorical) {
//        CsvRow last = rows.get(rows.size() - 1);
//        String subfix = result.success ? "_success" : "_failure";
//        File chartFile = new File(TEST_FOLDER + "/" + last.getSymbol() + subfix + ".png");
//        try (FileOutputStream output = new FileOutputStream(chartFile)) {
//            IChart<IRow> chart = new XYChart();
//            ChartGenerator.writeChart(transactions, output, chart);
//            ChartGenerator.writeChart(rows, output, chart);
//            ChartGenerator.writeChart(walletHistorical, output, chart);
//            ChartGenerator.save(output, chart);
//        } catch (IOException e) {}
//    }

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
