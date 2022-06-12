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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.exchange.Utils;
import com.jbescos.test.Simulation.Result;
import com.jbescos.test.util.TestFileStorage;

public class BotTest {
   
    private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());

    @Test
    public void allTogether() throws FileNotFoundException, IOException {
    	final String BASE_TEST_FOLDER = "./target/test-results-together/";
    	DateFormat format = new SimpleDateFormat(Utils.FORMAT);
    	List<Result> results = Collections.synchronizedList(new ArrayList<>());
    	List<CompletableFuture<?>> completables = new ArrayList<>();
    	final int MONTHS_INTERVAL = 24;
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
    		CloudProperties cloudProperties = new CloudProperties(userId, null);
    		String from = users[i + 1];
    		Date to = Utils.getStartOfSpecifiedMonth(Utils.fromString(format, from), MONTHS_INTERVAL);
    		String toStr = Utils.fromDate(Utils.FORMAT, to);
    		Simulation simulation = null;
    		while ((simulation = Simulation.build(from, toStr, BASE_TEST_FOLDER, cloudProperties)) != null) {
    		    final Simulation sim = simulation;
    		    CompletableFuture<Void> completable = CompletableFuture.supplyAsync(() -> sim.runTogether()).thenAccept(result -> {
    		    	if (result != null) {
    		    		results.add(result);
    		    	}
    		    });
    		    completables.add(completable);
    			from = toStr;
    			to = Utils.getStartOfSpecifiedMonth(Utils.fromString(format, from), MONTHS_INTERVAL);
    			toStr = Utils.fromDate(Utils.FORMAT, to);
    		}
    	}
    	CompletableFuture.allOf(completables.toArray(new CompletableFuture[0])).join();
    	if (!results.isEmpty()) {
	    	Collections.sort(results);
	    	TestFileStorage fileStorage = new TestFileStorage(BASE_TEST_FOLDER, null, null);
	    	StringBuilder builder = new StringBuilder();
	    	results.forEach(result -> builder.append(result.toCsv()));
	    	double avg = results.stream().mapToDouble(Result::getBenefitPercentage).average().getAsDouble();
	    	builder.append("AVG,").append(Utils.format(avg)).append("%").append(Utils.NEW_LINE);
	    	double avgBuyValue = results.stream().mapToDouble(Result::getBuyValue).average().getAsDouble();
	    	builder.append("BUY_VALUE_AVG,").append(Utils.format(avgBuyValue)).append(Utils.NEW_LINE);
	    	double min = results.stream().mapToDouble(Result::getBenefitPercentage).min().getAsDouble();
	    	builder.append("MIN,").append(Utils.format(min)).append(Utils.NEW_LINE);
	    	double max = results.stream().mapToDouble(Result::getBenefitPercentage).max().getAsDouble();
	    	builder.append("MAX,").append(Utils.format(max)).append(Utils.NEW_LINE);
	    	fileStorage.updateFile("results.csv", builder.toString().getBytes(), Result.CSV_HEAD.getBytes());
	    	LOGGER.info("AVG: " + Utils.format(avg) + "%");
    	}
    }
    
    @Test
    @Ignore
    public void separately() throws FileNotFoundException, IOException {
    	final String BASE_TEST_FOLDER = "./target/test-results-separately/";
    	DateFormat format = new SimpleDateFormat(Utils.FORMAT);
    	List<CompletableFuture<?>> completables = new ArrayList<>();
    	final int MONTHS_INTERVAL = 12;
    	String[] users = new String [] {
    			"kucoin-all", "2021-10-25",
    			"binance-all", "2021-05-08",
    			"ftx", "2021-11-09",
    			"okex", "2021-11-09"
    			};
    	for (int i = 0; i < users.length; i = i + 2) {
    		String userId = users[i];
    		CloudProperties cloudProperties = new CloudProperties(userId, null);
    		String from = users[i + 1];
    		Date to = Utils.getStartOfSpecifiedMonth(Utils.fromString(format, from), MONTHS_INTERVAL);
    		String toStr = Utils.fromDate(Utils.FORMAT, to);
    		Simulation simulation = null;
    		while ((simulation = Simulation.build(from, toStr, BASE_TEST_FOLDER, cloudProperties)) != null) {
    		    final Simulation sim = simulation;
    		    CompletableFuture<Void> completable = CompletableFuture.supplyAsync(() -> sim.runBySymbol()).thenAccept(scores -> {
    		    	if (scores != null) {
	    		    	Collections.sort(scores);
	    		    	StringBuilder builder = new StringBuilder("bot.white.list=");
	    		    	scores.stream().forEach(score -> builder.append(score.getSymbol()).append(","));
	    		    	LOGGER.info(userId + ": " + builder.toString());
    		    	}
    		    });
    		    completables.add(completable);
    			from = toStr;
    			to = Utils.getStartOfSpecifiedMonth(Utils.fromString(format, from), MONTHS_INTERVAL);
    			toStr = Utils.fromDate(Utils.FORMAT, to);
    		}
    	}
    	CompletableFuture.allOf(completables.toArray(new CompletableFuture[0])).join();
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
