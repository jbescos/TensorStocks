package com.jbescos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.cloudbot.Bot;
import com.jbescos.cloudbot.BotUtils;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.SymbolStats.Action;
import com.jbescos.common.Utils;

public class BotTest {
	
	private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());
	private static final long DAYS_BACK_MILLIS = 3600 * 1000 * 24 * 5;
	
	@Test
	@Ignore
	public void total() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("DOGEUSDT", "DOTUSDT", "BTTUSDT", "ADAUSDT", "XRPUSDT", "MATICUSDT", "CHZUSDT", "GRTUSDT", "ANKRUSDT", "ADAUSDT", "BNBUSDT", "CAKEUSDT", "BAKEUSDT", "SOLUSDT");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 141.0);
		wallet.put("DOGEUSDT", 348.25);
		wallet.put("BTTUSDT", 16336.0);
		check("/total.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "2021-05-10 08:33:48"));
	}
	
	@Test
	public void bnb() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("BNBUSDT");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 100.0);
		check("/BNBUSDT.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "2021-06-22 01:11:24"));
	}
	
	@Test
	public void doge() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("BNBUSDT");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 50.0);
		wallet.put("DOGEUSDT", 50.0);
		check("/DOGEUSDT.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "2021-05-27 02:40:40"));
	}
	
	@Test
	public void example1() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("SYMBOL");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 100.0);
		wallet.put("SYMBOL", 100.0);
		check("/example1.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "1970-01-05 07:00:00"));
	}
	
	@Test
	public void example2() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("SYMBOL");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 100.0);
		wallet.put("SYMBOL", 100.0);
		check("/example2.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "1970-01-05 07:00:00"));
	}
	
	@Test
	public void example3() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("SYMBOL");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 100.0);
		check("/example3.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "1970-01-05 07:00:00"));
	}
	
	@Test
	public void example4() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("SYMBOL");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 100.0);
		check("/example4.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "1970-01-05 07:00:00"));
	}
	
	@Test
	public void example5() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("SYMBOL");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 100.0);
		check("/example5.csv", wallet, cryptos, Utils.fromString(Utils.FORMAT_SECOND, "1970-01-05 07:00:00"));
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
		LOGGER.info("Trader: " + trader);
		LOGGER.info("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() >= holder.getUsdtSnapshot());
	}
	
	@Test
	public void round() {
		assertEquals("21330.888888", Utils.format(21330.888887878787));
	}
	
	@Test
	public void usdtPerUnit() {
		double quoteOrderQtyBD = Double.parseDouble("11.801812");
		double executedQtyBD = Double.parseDouble("0.47700000");
		double result = quoteOrderQtyBD/executedQtyBD;
		String resultStr = Utils.format(result);
		assertEquals("24.741744", resultStr);
	}
	
	@Test
	public void minSell() {
		// Good moment to sell
		final String SYMBOL_LIMIMTED = "test";
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
	
}
