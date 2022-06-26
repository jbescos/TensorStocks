package com.jbescos.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import com.jbescos.common.CloudProperties;
import com.jbescos.common.DefaultBrokerManager;
import com.jbescos.exchange.Broker;
import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.IRow;
import com.jbescos.exchange.Utils;
import com.jbescos.test.util.TestFileInMemoryStorage;

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
		String lastDay = DataLoader.lastDayCsv(cloudProperties.USER_EXCHANGE.name());
		if (lastDay != null) {
			Date lastTo = Utils.fromString(Utils.FORMAT, lastDay);
			Date fromDate = Utils.fromString(Utils.FORMAT, from);
			if (fromDate.getTime() < lastTo.getTime()) {
				return new Simulation(from, to, baseTestFolder, cloudProperties);
			}
		}
		return null;
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
        List<CsvTransactionRow> openTransactions = new ArrayList<>();
        List<CsvProfitRow> profit = new ArrayList<>();
    	try {
	    	while (now <= last) {
	    		long previous = now - hoursBackMillis;
	    		List<CsvRow> segment = symbol == null ? loader.get(previous, now) : loader.get(symbol, previous, now);
//	    		TestFileStorage fileManager = new TestFileStorage(testFolder + (symbol == null ? "/total_" : "/" + symbol + "_"), transactions, segment);
	    		TestFileInMemoryStorage fileManager = new TestFileInMemoryStorage(transactions, openTransactions, segment, profit);
	    	    List<Broker> stats = new DefaultBrokerManager(loader.getCloudProperties(), fileManager).loadBrokers();
		    	BotExecution trader = BotExecution.test(loader.getCloudProperties(), fileManager, wallet, walletHistorical, loader.getCloudProperties().MIN_TRANSACTION);
	            trader.execute(stats);
	    	    CsvRow lastRow = segment.get(segment.size() - 1);
	    	    CsvRow next = loader.next(lastRow.getSymbol(), lastRow);
	    	    if (next != null) {
	    	        now = next.getDate().getTime();
	    	    } else {
	    	        break;
	    	    }
	    	}
	    	TestFileInMemoryStorage fileManager = new TestFileInMemoryStorage(transactions, openTransactions, null, profit);
	    	fileManager.persist(testFolder);
    	} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error in " + testFolder, e);
			throw new RuntimeException("Error in " + testFolder, e);
		}
	}
	
	public Result runTogether() {
		try {
			load();
			if (loader.first() != null) {
				final double INITIAL_USDT = 10000;
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
		        	IChart<IRow> chart = new DateChart();
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
		        List<CsvTransactionRow> buys = transactions.stream().filter(tx -> tx.getSide() == Action.BUY).collect(Collectors.toList());
		        double buyValue = (totalPrice - INITIAL_USDT) / buys.size();
		        Map<String, List<CsvTransactionRow>> txsBySymbol = transactions.stream().collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
		        txsBySymbol.entrySet().parallelStream().forEach(entry -> {
		        	List<CsvRow> data = loader.get(entry.getKey());
		        	File chartF = new File(testFolder + "/z_" + entry.getKey() + ".png");
		        	entry.getValue().stream().forEach(tx -> tx.setUsdt(tx.getUsdtUnit()));
		        	try (FileOutputStream output = new FileOutputStream(chartF)) {
		        		IChart<IRow> chart = new DateChart();
		        		ChartGenerator.writeChart(entry.getValue(), output, chart);
		        		ChartGenerator.writeChart(data, output, chart);
		        		ChartGenerator.save(output, chart);
		        	} catch (IOException e) {
		        		e.printStackTrace();
		        	}
		        });
		        return new Result(INITIAL_USDT, totalPrice, benefit, from, to, loader.getCloudProperties().USER_ID, loader.getCloudProperties().USER_EXCHANGE.name(), buys.size(), buyValue);
			} else {
				return null;
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error in " + testFolder, e);
			throw new RuntimeException("Error in " + testFolder, e);
		}
	}
	
	public static class Score implements Comparable<Score> {
		private final int profit;
		private final String symbol;

		public Score(int profit, String symbol) {
			this.profit = profit;
			this.symbol = symbol;
		}
		public int getProfit() {
			return profit;
		}
		public String getSymbol() {
			return symbol;
		}
		@Override
		public int compareTo(Score o) {
			return Integer.compare(o.getProfit(), getProfit());
		}
		
	}
	
	public static class Result implements Comparable<Result> {
		
		public static final String CSV_HEAD = "FROM,TO,EXCHANGE,USER_ID,INITIAL,FINAL,BUYS,BUY_VALUE,BENEFIT" + Utils.NEW_LINE;
		private final double initialAmount;
		private final double finalAmount;
		private final double benefitPercentage;
		private final String from;
		private final String to;
		private final String userId;
		private final String exchange;
		private final int purchases;
		private final double buyValue;

		public Result(double initialAmount, double finalAmount, double benefitPercentage, String from, String to,
				String userId, String exchange, int purchases, double buyValue) {
			this.initialAmount = initialAmount;
			this.finalAmount = finalAmount;
			this.benefitPercentage = benefitPercentage;
			this.from = from;
			this.to = to;
			this.userId = userId;
			this.exchange = exchange;
			this.purchases = purchases;
			this.buyValue = buyValue;
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

		public double getBuyValue() {
			return buyValue;
		}

		public String toCsv() {
			StringBuilder builder = new StringBuilder();
			builder.append(from).append(",").append(to).append(",").append(exchange).append(",").append(userId).append(",")
			.append(Utils.format(initialAmount)).append("$,").append(Utils.format(finalAmount)).append("$,").append(purchases)
			.append("$,").append(Utils.format(buyValue))
			.append(",").append(Utils.format(benefitPercentage))
			.append("%").append(Utils.NEW_LINE);
			return builder.toString();
		}

		@Override
		public int compareTo(Result o) {
			int result = from.compareTo(o.from);
			if (result == 0) {
				result = userId.compareTo(o.userId);
			}
			return result;
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
            return row.getFearGreedIndexAvg();
        }

        @Override
        public Double getAvg2() {
            return null;
        }
        
    }
	
}
