package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Test;

import com.jbescos.cloudbot.Bot;
import com.jbescos.cloudbot.BotUtils;
import com.jbescos.cloudchart.ChartGenerator;
import com.jbescos.cloudchart.IChart;
import com.jbescos.cloudchart.XYChart;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.SymbolStats.Action;
import com.jbescos.common.Utils;

public class BotTest {
	
	private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());
	private static final long DAYS_BACK_MILLIS = Long.parseLong(CloudProperties.BOT_DAYS_BACK_STATISTICS) * 3600 * 1000 * 24;
	private static final List<TestResult> results = new ArrayList<>();
	
	@AfterClass
	public static void afterClass() {
		LOGGER.info(results.toString());
	}
	
	@Test
	public void ada() throws FileNotFoundException, IOException {
		total("ADAUSDT", "/ADAUSDT.csv", 1.65);
		total("ADAUSDT", "/reversed_ADAUSDT.csv", 1.46);
	}
	
	@Test
	public void ankr() throws FileNotFoundException, IOException {
		total("ANKRUSDT", "/ANKRUSDT.csv", 0.17);
		total("ANKRUSDT", "/reversed_ANKRUSDT.csv", 0.1);
	}
	
	@Test
	public void bake() throws FileNotFoundException, IOException {
		total("BAKEUSDT", "/BAKEUSDT.csv", 6.75);
		total("BAKEUSDT", "/reversed_BAKEUSDT.csv", 4.3);
	}
	
	@Test
	public void bnb() throws FileNotFoundException, IOException {
		total("BNBUSDT", "/BNBUSDT.csv", 638);
		total("BNBUSDT", "/reversed_BNBUSDT.csv", 320);
	}
	
	@Test
	public void btc() throws FileNotFoundException, IOException {
		total("BTCUSDT", "/BTCUSDT.csv", 59000);
		total("BTCUSDT", "/reversed_BTCUSDT.csv", 35000);
	}
	
	@Test
	public void btt() throws FileNotFoundException, IOException {
		total("BTTUSDT", "/BTTUSDT.csv", 0.0079);
		total("BTTUSDT", "/reversed_BTTUSDT.csv", 0.0036);
	}
	
	@Test
	public void cake() throws FileNotFoundException, IOException {
		total("CAKEUSDT", "/CAKEUSDT.csv", 38);
		total("CAKEUSDT", "/reversed_CAKEUSDT.csv", 15.5);
	}
	
	@Test
	public void chz() throws FileNotFoundException, IOException {
		total("CHZUSDT", "/CHZUSDT.csv", 0.5);
		total("CHZUSDT", "/reversed_CHZUSDT.csv", 0.28);
	}
	
	@Test
	public void doge() throws FileNotFoundException, IOException {
		total("DOGEUSDT", "/DOGEUSDT.csv", 0.7);
		total("DOGEUSDT", "/reversed_DOGEUSDT.csv", 0.3);
	}
	
	@Test
	public void dot() throws FileNotFoundException, IOException {
		total("DOTUSDT", "/DOTUSDT.csv", 40);
		total("DOTUSDT", "/reversed_DOTUSDT.csv", 20.5);
	}
	
	@Test
	public void grt() throws FileNotFoundException, IOException {
		total("GRTUSDT", "/GRTUSDT.csv", 1.6);
		total("GRTUSDT", "/reversed_GRTUSDT.csv", 0.6);
	}
	
	@Test
	public void matic() throws FileNotFoundException, IOException {
		total("MATICUSDT", "/MATICUSDT.csv", 0.75);
		total("MATICUSDT", "/reversed_MATICUSDT.csv", 1.8);
	}
	
	@Test
	public void shib() throws FileNotFoundException, IOException {
		total("SHIBUSDT", "/SHIBUSDT.csv", 0.000035);
		total("SHIBUSDT", "/reversed_SHIBUSDT.csv", 0.000008);
	}
	
	@Test
	public void sol() throws FileNotFoundException, IOException {
		total("SOLUSDT", "/SOLUSDT.csv", 44);
		total("SOLUSDT", "/reversed_SOLUSDT.csv", 28.5);
	}
	
	@Test
	public void xrp() throws FileNotFoundException, IOException {
		total("XRPUSDT", "/XRPUSDT.csv", 1.6);
		total("XRPUSDT", "/reversed_XRPUSDT.csv", 0.85);
	}
	
	@Test
	public void eth() throws FileNotFoundException, IOException {
		total("ETHUSDT", "/ETHUSDT.csv", 3600);
		total("ETHUSDT", "/reversed_ETHUSDT.csv", 2400);
	}
	
	private void total(String symbol, String csv, double initialUSDT) throws IOException {
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", initialUSDT);
//		wallet.put(symbol, 1.0);
		check(csv, wallet, null, Utils.fromString(Utils.FORMAT_SECOND, "2021-05-12 08:33:48"));
	}
	
	private void check(String csv, Map<String, Double> wallet, List<String> cryptos, Date now) throws IOException {
		Bot trader = new Bot(wallet, false, cryptos);
		Bot holder = new Bot(new HashMap<>(wallet), true, cryptos);
		List<CsvRow> rows = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream(csv)))) {
			rows = CsvUtil.readCsvRows(true, ",", reader);
		}
		while (true) {
			Date to = new Date(now.getTime());
			// Days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			List<CsvRow> segment = rows.stream().filter(row -> row.getDate().getTime() >= from.getTime() && row.getDate().getTime() < to.getTime()).collect(Collectors.toList());
//			LOGGER.info("Loading data from " + Utils.fromDate(Utils.FORMAT_SECOND, from) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, to) + ". " + segment.size() + " records");
			if (segment.isEmpty()) {
				break;
			}
			List<SymbolStats> stats = BotUtils.fromCsvRows(segment, trader.getTransactions());
			trader.execute(stats);
			holder.execute(stats);
			now = new Date(now.getTime() + (1000 * 60 * 30));
		}
		TestResult result = new TestResult(csv, trader.getUsdtSnapshot(), holder.getUsdtSnapshot());
		chart(csv, trader, result, cryptos);
		results.add(result);
	}
	
	private void chart(String csv, Bot trader, TestResult result, List<String> cryptos) throws IOException {
		List<? extends IRow> rows = null;
		try (InputStream input = BotTest.class.getResourceAsStream(csv);
				InputStreamReader inputReader = new InputStreamReader(input);
				BufferedReader reader = new BufferedReader(inputReader);) {
			rows = CsvUtil.readCsvRows(true, ",", reader);
		}
		File chartFile = new File("./target/" + csv + (result.success ? "_success_" : "_failure_") + ".png");
		if (chartFile.exists()) {
			chartFile.delete();
		}
		rows = rows.stream().filter(row -> cryptos == null || cryptos.contains(row.getLabel())).collect(Collectors.toList());
		try (FileOutputStream output = new FileOutputStream(chartFile)) {
			IChart<IRow> chart = new XYChart();
			ChartGenerator.writeChart(rows, output, chart);
			ChartGenerator.writeChart(trader.getWalletHistorical(), output, chart);
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
		double result = quoteOrderQtyBD/executedQtyBD;
		String resultStr = Utils.format(result);
		assertEquals("24.74174423", resultStr);
	}
	
	@Test
	public void minSell() {
		// Good moment to sell
		final String SYMBOL_LIMIMTED = "TESTUSDT";
		List<CsvRow> rows = Arrays.asList(new CsvRow(new Date(0), SYMBOL_LIMIMTED, 1.0), new CsvRow(new Date(50000), SYMBOL_LIMIMTED, 100.0), new CsvRow(new Date(100000), SYMBOL_LIMIMTED, 99.0));
		SymbolStats stats = BotUtils.fromCsvRows(rows, Collections.emptyList()).get(0);
		assertEquals(Action.NOTHING, stats.getAction());
		final String SYMBOL_NOT_LIMIMTED = "unlimitedSymbol";
		rows = Arrays.asList(new CsvRow(new Date(0), SYMBOL_NOT_LIMIMTED, 1.0), new CsvRow(new Date(50000), SYMBOL_NOT_LIMIMTED, 100.0), new CsvRow(new Date(100000), SYMBOL_NOT_LIMIMTED, 99.0));
		stats = BotUtils.fromCsvRows(rows, Collections.emptyList()).get(0);
		assertEquals(Action.SELL, stats.getAction());
	}
	
	private Map<String, Double> createWallet(double amount, List<String> cryptos){
		Map<String, Double> wallet = new HashMap<>();
		wallet.put(Utils.USDT, amount);
		for (String crypto: cryptos) {
			wallet.put(crypto, amount);
		}
		return wallet;
	}
	
	private static class TestResult {
		private final String csv;
		private final double trader;
		private final double holder;
		private final double absoluteBenefit;
		private final double multiplier;
		private final boolean success;
		
		public TestResult(String csv, double trader, double holder) {
			this.csv = csv;
			this.trader = trader;
			this.holder = holder;
			this.absoluteBenefit = trader - holder;
			this.multiplier = trader / holder;
			this.success = absoluteBenefit >= 0;
		}

		@Override
		public String toString() {
			return "\n TestResult [csv=" + csv + ", trader=" + trader + ", holder=" + holder + ", absoluteBenefit="
					+ absoluteBenefit + ", multiplier=" + multiplier + ", success=" + success + "]";
		}
		
	}
	
}
