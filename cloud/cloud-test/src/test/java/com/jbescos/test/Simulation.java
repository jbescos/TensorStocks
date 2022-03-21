package com.jbescos.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.cloudbot.BotExecution;
import com.jbescos.cloudchart.ChartGenerator;
import com.jbescos.cloudchart.DateChart;
import com.jbescos.cloudchart.IChart;
import com.jbescos.cloudchart.XYChart;
import com.jbescos.common.Broker;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.DefaultBrokerManager;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;
import com.jbescos.test.util.TestFileStorage;

public class Simulation {

	private static final Logger LOGGER = Logger.getLogger(Simulation.class.getName());
	private static final long HOURS_MILLIS = 3600 * 1000;
	private final DataLoader loader;
	private final String testFolder;
	private final long hoursBackMillis;
	private final String from;
	private final String to;
	private boolean loaded;
	private double benefit;

	private Simulation(String from, String to, String baseTestFolder, CloudProperties cloudProperties) {
		this.loader = new DataLoader(from, to, cloudProperties);
		this.from = from;
		this.to = to;
		String testFolder = baseTestFolder + cloudProperties.USER_ID + "/" + from;
		if (to != null) {
			testFolder = testFolder + "_" + to;
		}
		testFolder = testFolder + "/";
		this.testFolder = testFolder;
		this.hoursBackMillis = loader.getCloudProperties().BOT_HOURS_BACK_STATISTICS * HOURS_MILLIS;
	}
	
	public static Simulation build(String from, String to, String baseTestFolder, CloudProperties cloudProperties) {
		Date lastTo = Utils.fromString(Utils.FORMAT, DataLoader.lastDayCsv(cloudProperties.USER_EXCHANGE.name()));
		Date fromDate = Utils.fromString(Utils.FORMAT, from);
		if (fromDate.getTime() < lastTo.getTime()) {
			return new Simulation(from, to, baseTestFolder, cloudProperties);
		} else {
			return null;
		}
	}
	
	private void load() throws IOException {
		if (!loaded) {
			loaded = true;
			loader.loadData();
		}
	}
	
	private void iterate(Map<String, Double> wallet, List<CsvRow> walletHistorical, List<CsvTransactionRow> transactions, String symbol) {
		CsvRow first = symbol == null ? loader.first() : loader.first(symbol);
    	long now = first.getDate().getTime() + hoursBackMillis;
    	long last = loader.last(first.getSymbol()).getDate().getTime();
    	try {
	    	while (now <= last) {
	    		long previous = now - hoursBackMillis;
	    		List<CsvRow> segment = symbol == null ? loader.get(previous, now) : loader.get(symbol, previous, now);
	    		TestFileStorage fileManager = new TestFileStorage(testFolder + (symbol == null ? "/total_" : "/" + symbol + "_"), transactions, segment);
	    	    List<Broker> stats = new DefaultBrokerManager(loader.getCloudProperties(), fileManager).loadBrokers();
		    	BotExecution trader = BotExecution.test(loader.getCloudProperties(), fileManager, wallet, transactions, walletHistorical, loader.getCloudProperties().MIN_TRANSACTION);
	            trader.execute(stats);
	    	    CsvRow lastRow = segment.get(segment.size() - 1);
	    	    CsvRow next = loader.next(lastRow.getSymbol(), lastRow);
	    	    if (next != null) {
	    	        now = next.getDate().getTime();
	    	    } else {
	    	        break;
	    	    }
	    	}
    	} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error in " + testFolder, e);
			throw new RuntimeException("Error in " + testFolder, e);
		}
	}
	
	public Result runTogether() {
		try {
			load();
			final double INITIAL_USDT = 1000;
			Map<String, Double> wallet = new HashMap<>();
	        wallet.put(Utils.USDT, INITIAL_USDT);
	        List<CsvRow> walletHistorical = new ArrayList<>();
	    	List<CsvTransactionRow> transactions = new ArrayList<>();
	    	iterate(wallet, walletHistorical, transactions, null);
	    	List<CsvRow> totalWalletHistorical = walletHistorical.stream().filter(row -> row.getSymbol().startsWith("TOTAL")).collect(Collectors.toList());
	    	double totalPrice = totalWalletHistorical.get(totalWalletHistorical.size() - 1).getPrice();
	    	benefit = ((totalPrice * 100) / INITIAL_USDT) - 100;
	    	LOGGER.info(() -> loader.cloudProperties.USER_ID + " from " + from + " to " + to + " " + Utils.format(benefit) + "%, " + Utils.format(totalPrice) + " " + Utils.USDT);
	    	File chartFile = new File(testFolder + "/total.png");
	    	Path path = Paths.get(testFolder);
	    	Files.createDirectories(path);
	        try (FileOutputStream output = new FileOutputStream(chartFile)) {
	        	IChart<IRow> chart = new XYChart();
	        	ChartGenerator.writeChart(walletHistorical, output, chart);
	            ChartGenerator.save(output, chart);
	        }
	        chartFile = new File(testFolder + "/fearGreed.png");
	        List<IRow> fears = loader.get(loader.first().getSymbol()).stream().map(row -> new FearGreedRow(row)).collect(Collectors.toList());
	        try (FileOutputStream output = new FileOutputStream(chartFile)) {
	            IChart<IRow> chart = new DateChart();
	            ChartGenerator.writeChart(fears, output, chart);
	            ChartGenerator.save(output, chart);
	        }
	        return new Result(INITIAL_USDT, totalPrice, benefit, from, to, loader.getCloudProperties().USER_ID, loader.getCloudProperties().USER_EXCHANGE.name());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error in " + testFolder, e);
			throw new RuntimeException("Error in " + testFolder, e);
		}
	}
	
	public void runBySymbol() {
		try {
			load();
			Collection<String> symbols = loader.symbols();
			symbols.parallelStream().forEach(symbol -> {
				List<CsvTransactionRow> transactions = new ArrayList<>();
				List<CsvRow> walletHistorical = new ArrayList<>();
				CsvRow first = loader.first(symbol);
				Map<String, Double> wallet = new HashMap<>();
		        wallet.put(Utils.USDT, first.getPrice());
		        iterate(wallet, walletHistorical, transactions, symbol);
		        List<CsvRow> total = loader.get(symbol);
		        List<CsvRow> totalWalletHistorical = walletHistorical.stream().filter(row -> row.getSymbol().startsWith("TOTAL")).collect(Collectors.toList());
		        symbolCharts(symbol, total, transactions, totalWalletHistorical);
			});
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error in " + testFolder, e);
			throw new RuntimeException("Error in " + testFolder, e);
		}
	}
	
	private void symbolCharts(String symbol, List<CsvRow> rows, List<CsvTransactionRow> transactions, List<CsvRow> walletHistorical) {
		File chartFile = new File(testFolder + "/" + symbol + ".png");
        try (FileOutputStream output = new FileOutputStream(chartFile)) {
            IChart<IRow> chart = new XYChart();
            ChartGenerator.writeChart(transactions, output, chart);
            ChartGenerator.writeChart(rows, output, chart);
            ChartGenerator.writeChart(walletHistorical, output, chart);
            ChartGenerator.save(output, chart);
        } catch (IOException e) {}
	}
	
	public static class Result implements Comparable<Result> {
		
		public static final String CSV_HEAD = "FROM,TO,EXCHANGE,USER_ID,INITIAL,FINAL,BENEFIT" + Utils.NEW_LINE;
		private final double initialAmount;
		private final double finalAmount;
		private final double benefitPercentage;
		private final String from;
		private final String to;
		private final String userId;
		private final String exchange;

		public Result(double initialAmount, double finalAmount, double benefitPercentage, String from, String to,
				String userId, String exchange) {
			this.initialAmount = initialAmount;
			this.finalAmount = finalAmount;
			this.benefitPercentage = benefitPercentage;
			this.from = from;
			this.to = to;
			this.userId = userId;
			this.exchange = exchange;
		}

		public double getInitialAmount() {
            return initialAmount;
        }

        public double getFinalAmount() {
            return finalAmount;
        }

        public double getBenefitPercentage() {
            return benefitPercentage;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getUserId() {
            return userId;
        }

        public String getExchange() {
            return exchange;
        }

		public String toCsv() {
			StringBuilder builder = new StringBuilder();
			builder.append(from).append(",").append(to).append(",").append(exchange).append(",").append(userId).append(",")
			.append(Utils.format(initialAmount)).append("$,").append(Utils.format(finalAmount)).append("$,").append(Utils.format(benefitPercentage))
			.append("%").append(Utils.NEW_LINE);
			return builder.toString();
		}

		@Override
		public int compareTo(Result o) {
			return from.compareTo(o.from);
		}
		
	}
	
    private static class FearGreedRow implements IRow {

        private final CsvRow row;

        public FearGreedRow(CsvRow row) {
            this.row = row;
        }
        @Override
        public Date getDate() {
            return row.getDate();
        }

        @Override
        public double getPrice() {
            return row.getFearGreedIndex();
        }

        @Override
        public String getLabel() {
            return "FEAR-GREED-INDEX";
        }

        @Override
        public Double getAvg() {
            return null;
        }

        @Override
        public Double getAvg2() {
            return null;
        }
        
    }
	
}
