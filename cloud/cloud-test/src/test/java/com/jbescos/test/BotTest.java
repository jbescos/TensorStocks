package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Test;

import com.jbescos.cloudbot.Bot;
import com.jbescos.cloudbot.BotUtils;
import com.jbescos.cloudchart.BarChart;
import com.jbescos.cloudchart.ChartGenerator;
import com.jbescos.cloudchart.IChart;
import com.jbescos.cloudchart.XYChart;
import com.jbescos.common.BuySellAnalisys;
import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class BotTest {

	private static final boolean TEST_REVERSE = false;
	private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());
	private static final long DAY_MILLIS = 3600 * 1000 * 24;
	private static final long DAYS_BACK_MILLIS = CloudProperties.BOT_DAYS_BACK_STATISTICS * DAY_MILLIS;
	private static final long DAYS_BACK_TRANSACTIONS_MILLIS = CloudProperties.BOT_DAYS_BACK_TRANSACTIONS * DAY_MILLIS;
	private static final List<TestResult> results = new ArrayList<>();

	@AfterClass
	public static void afterClass() {
		LOGGER.info(results.toString());
		double total = 0;
		for (TestResult result : results) {
			total = total + result.multiplier;
		}
		LOGGER.info("Total multiplier: " + (total / results.size()));
	}

	@Test
	public void realData() throws IOException {
		Date from = new Date();
		Date to = Utils.fromString(Utils.FORMAT, "2021-05-08");
		int daysBetween = (int) ChronoUnit.DAYS.between(to.toInstant(), from.toInstant());
		List<String> days = Utils.daysBack(from, daysBetween, "/", ".csv"); // Starts the 2021-05-08
		List<CsvRow> rows = new ArrayList<>();
		for (String day : days) {
			String csvFile = day;
			InputStream csv = CsvUtilTest.class.getResourceAsStream(csvFile);
			if (csv != null) {
				LOGGER.info("Loading " + csvFile);
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv))) {
					List<CsvRow> dailyRows = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
					dailyRows = dailyRows.stream().filter(r -> CloudProperties.BOT_WHITE_LIST_SYMBOLS.isEmpty() || CloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(r.getSymbol())).collect(Collectors.toList());
					rows.addAll(dailyRows);
				}
				LOGGER.info("Rows loaded so far " + rows.size());
			}
		}
		Map<String, List<CsvRow>> grouped = rows.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		rows = null;
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			CsvRow first = entry.getValue().get(0);
			Map<String, Double> wallet = new HashMap<>();
			wallet.put("USDT", first.getPrice());
			Date start = new Date(first.getDate().getTime() + DAY_MILLIS);
			check(entry.getValue(), wallet, start);
			if (TEST_REVERSE) {
				reverse(entry.getValue());
				wallet = new HashMap<>();
				wallet.put("USDT", entry.getValue().get(0).getPrice());
				check(entry.getValue(), wallet, start);
			}
		}
	}
	
	private void reverse(List<CsvRow> rows) {
		for (int i=0; i<(rows.size()/2);i++) {
			CsvRow row0 = rows.get(i);
			Date date0 = row0.getDate();
			CsvRow rowLast = rows.get((rows.size() - 1) - i);
			row0.setDate(rowLast.getDate());
			rowLast.setDate(date0);
		}
		Collections.reverse(rows);
		Double previousResult = null;
		for (CsvRow row : rows) {
			row.setSymbol(row.getSymbol() + "-reversed");
			previousResult = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), previousResult);
			row.setAvg(previousResult);
		}
	}

	private void check(List<CsvRow> rows, Map<String, Double> wallet, Date now) throws IOException {
		Bot trader = new Bot(wallet, false);
		Bot holder = new Bot(new HashMap<>(wallet), true);
		while (true) {
			Date to = new Date(now.getTime());
			// Days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			List<CsvRow> segment = rows.stream()
					.filter(row -> row.getDate().getTime() >= from.getTime() && row.getDate().getTime() < to.getTime())
					.collect(Collectors.toList());
//			LOGGER.info("Loading data from " + Utils.fromDate(Utils.FORMAT_SECOND, from) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, to) + ". " + segment.size() + " records");
			if (segment.isEmpty()) {
				break;
			}
			Date fromTx = new Date(now.getTime() - DAYS_BACK_TRANSACTIONS_MILLIS);
			List<CsvTransactionRow> transactions = trader.getTransactions().stream().filter(row -> row.getDate().getTime() >= fromTx.getTime()).collect(Collectors.toList());
			List<BuySellAnalisys> stats = BotUtils.fromCsvRows(segment, transactions);
			trader.execute(stats);
			holder.execute(stats);
			now = new Date(now.getTime() + (1000 * 60 * 30));
		}
		CsvRow first = rows.get(0);
		TestResult result = new TestResult(first.getSymbol(), trader.getUsdtSnapshot(), holder.getUsdtSnapshot());
		chart(rows, trader, result);
		results.add(result);
	}

	private void chart(List<CsvRow> rows, Bot trader, TestResult result) throws IOException {
		CsvRow last = rows.get(rows.size() - 1);
		String subfix = result.success ? "_success" : "_failure";
		File chartFile = new File("./target/" + last.getSymbol() + subfix + ".png");
		if (chartFile.exists()) {
			chartFile.delete();
		}
		try (FileOutputStream output = new FileOutputStream(chartFile)) {
			IChart<IRow> chart = new XYChart();
			ChartGenerator.writeChart(rows, output, chart);
			ChartGenerator.writeChart(trader.getWalletHistorical(), output, chart);
			ChartGenerator.writeChart(trader.getTransactions(), output, chart);
			ChartGenerator.save(output, chart);
		}
		File barChartFile = new File("./target/" + last.getSymbol() + subfix + "_bar.png");
		if (barChartFile.exists()) {
			barChartFile.delete();
		}
		try (FileOutputStream output = new FileOutputStream(barChartFile)) {
			Map<String, Double> walletUsdt = new HashMap<>();
			for (Entry<String, Double> entry : trader.getWallet().entrySet()) {
				walletUsdt.put(entry.getKey(),  entry.getValue() * last.getPrice());
			}
			IChart<IRow> chart = new BarChart(walletUsdt);
			ChartGenerator.writeChart(trader.getTransactions(), output, chart);
			ChartGenerator.save(output, chart);
		}
	}

	@Test
	public void round() {
		assertEquals("21330.88888788", Utils.format(21330.888887878787));
	}

	@Test
	public void usdtPerUnit() {
		double quoteOrderQtyBD = Double.parseDouble("11.801812");
		double executedQtyBD = Double.parseDouble("0.47700000");
		double result = quoteOrderQtyBD / executedQtyBD;
		String resultStr = Utils.format(result);
		assertEquals("24.74174423", resultStr);
	}

	@Test
	public void minSell() {
		// Good moment to sell
		final String SYMBOL_LIMIMTED = "TESTUSDT";
		List<CsvRow> rows = Arrays.asList(new CsvRow(new Date(0), SYMBOL_LIMIMTED, 1.0),
				new CsvRow(new Date(50000), SYMBOL_LIMIMTED, 100.0),
				new CsvRow(new Date(100000), SYMBOL_LIMIMTED, 99.0));
		BuySellAnalisys stats = BotUtils.fromCsvRows(rows, Collections.emptyList()).get(0);
		assertEquals(Action.NOTHING, stats.getAction());
		final String SYMBOL_NOT_LIMIMTED = "unlimitedSymbol";
		rows = Arrays.asList(new CsvRow(new Date(0), SYMBOL_NOT_LIMIMTED, 1.0),
				new CsvRow(new Date(50000), SYMBOL_NOT_LIMIMTED, 100.0),
				new CsvRow(new Date(100000), SYMBOL_NOT_LIMIMTED, 99.0));
		stats = BotUtils.fromCsvRows(rows, Collections.emptyList()).get(0);
		assertEquals(Action.SELL, stats.getAction());
	}

	private Map<String, Double> createWallet(double amount, List<String> cryptos) {
		Map<String, Double> wallet = new HashMap<>();
		wallet.put(Utils.USDT, amount);
		for (String crypto : cryptos) {
			wallet.put(crypto, amount);
		}
		return wallet;
	}

	private static class TestResult {
		private final String symbol;
		private final double trader;
		private final double holder;
		private final double absoluteBenefit;
		private final double multiplier;
		private final boolean success;

		public TestResult(String symbol, double trader, double holder) {
			this.symbol = symbol;
			this.trader = trader;
			this.holder = holder;
			this.absoluteBenefit = trader - holder;
			this.multiplier = trader / holder;
			this.success = absoluteBenefit >= 0;
		}

		@Override
		public String toString() {
			return "\n TestResult [symbol=" + symbol + ", trader=" + trader + ", holder=" + holder + ", absoluteBenefit="
					+ absoluteBenefit + ", multiplier=" + multiplier + ", success=" + success + "]";
		}

	}

}
