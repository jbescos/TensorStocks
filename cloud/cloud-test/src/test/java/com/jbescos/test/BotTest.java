package com.jbescos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Test;

import com.jbescos.cloudbot.Bot;
import com.jbescos.cloudbot.BotUtils;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.Utils;

public class BotTest {
	
	private static final Logger LOGGER = Logger.getLogger(BotTest.class.getName());
	private static final long DAYS_BACK_MILLIS = 3600 * 1000 * 24 * 5;
	
	@Test
	public void simple() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("DOGEUSDT");
		Map<String, Double> wallet = createWallet(100, cryptos);
		Bot trader = new Bot(wallet, false, cryptos);
		Bot holder = new Bot(new HashMap<>(wallet), true, cryptos);
		Date limit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-15 15:33:48");
		Date now = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-11 08:33:48");
		while (now.getTime() < limit.getTime()) {
			Date to = new Date(now.getTime() + (3600 * 1000));
			// Days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream("/simple.csv")))) {
				List<CsvRow> csv = CsvUtil.readCsvRows(true, ",", reader, from, to);
				List<SymbolStats> stats = BotUtils.fromCsvRows(csv);
				trader.execute(stats);
				holder.execute(stats);
			}
			now = to;
			if (trader.isDidAction() && trader.getUsdtSnapshot() < holder.getUsdtSnapshot()) {
				LOGGER.warning("Trader: " + trader);
				LOGGER.warning("Holder: " + holder);
			}
		}
		LOGGER.info("Trader: " + trader);
		LOGGER.info("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() >= holder.getUsdtSnapshot());
	}
	
	@Test
	public void complex() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("DOGEUSDT", "DOTUSDT", "BTTUSDT", "ADAUSDT", "XRPUSDT", "MATICUSDT", "CHZUSDT", "GRTUSDT", "ANKRUSDT", "ADAUSDT");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 141.0);
		wallet.put("DOGEUSDT", 348.25);
		wallet.put("BTTUSDT", 16336.0);
		Bot trader = new Bot(wallet, false, cryptos);
		Bot holder = new Bot(new HashMap<>(wallet), true, cryptos);
		Date limit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-16 05:00:07");
		Date now = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-13 08:33:48");
		List<CsvRow> rows = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream("/total.csv")))) {
			rows = CsvUtil.readCsvRows(true, ",", reader);
		}
		while (now.getTime() < limit.getTime()) {
			Date to = new Date(now.getTime());
			// Days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			List<CsvRow> segment = rows.stream().filter(row -> row.getDate().getTime() >= from.getTime() && row.getDate().getTime() < to.getTime()).collect(Collectors.toList());
			List<SymbolStats> stats = BotUtils.fromCsvRows(segment);
			trader.execute(stats);
			holder.execute(stats);
			now = new Date(now.getTime() + 3600000);
			if (trader.isDidAction() && trader.getUsdtSnapshot() < holder.getUsdtSnapshot()) {
				LOGGER.warning("Trader: " + trader);
				LOGGER.warning("Holder: " + holder);
			}
		}
		LOGGER.info("Trader: " + trader);
		LOGGER.info("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() >= holder.getUsdtSnapshot());
	}
	
	@Test
	public void round() {
		assertEquals("21330.888888", String.format(Locale.US, "%.6f", 21330.888887878787));
	}
	
	@Test
	public void usdtPerUnit() {
		double quoteOrderQtyBD = Double.parseDouble("11.801812");
		double executedQtyBD = Double.parseDouble("0.47700000");
		double result = quoteOrderQtyBD/executedQtyBD;
		String resultStr = String.format(Locale.US, "%.6f", result);
		assertEquals("24.741744", resultStr);
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
