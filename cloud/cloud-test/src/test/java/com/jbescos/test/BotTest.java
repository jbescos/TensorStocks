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
import java.util.Map;

import org.junit.Test;

import com.jbescos.cloudbot.Bot;
import com.jbescos.cloudbot.BotUtils;
import com.jbescos.cloudbot.SymbolStats;
import com.jbescos.common.Utils;

public class BotTest {
	
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
				List<SymbolStats> stats = BotUtils.loadPredictions(from, to, reader, false);
				trader.execute(stats);
				holder.execute(stats);
			}
			now = to;
			if (trader.isDidAction() && trader.getUsdtSnapshot() < holder.getUsdtSnapshot()) {
				System.out.println("WARNING: \n Trader: " + trader + "\n Holder: " + holder);
			}
		}
		System.out.println("Trader: " + trader);
		System.out.println("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() > holder.getUsdtSnapshot());
	}
	
	@Test
	public void complex() throws FileNotFoundException, IOException {
		List<String> cryptos = Arrays.asList("DOGEUSDT", "DOTUSDT", "BTTUSDT", "ADAUSDT", "XRPUSDT", "MATICUSDT", "CHZUSDT", "GRTUSDT", "ANKRUSDT", "SHIBUSDT", "ADAUSDT");
		Map<String, Double> wallet = new HashMap<>();
		wallet.put("USDT", 141.0);
		wallet.put("DOGEUSDT", 348.25);
		wallet.put("BTTUSDT", 16336.0);
		Bot trader = new Bot(wallet, false, cryptos);
		Bot holder = new Bot(new HashMap<>(wallet), true, cryptos);
		Date limit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-16 05:00:07");
		Date now = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-13 08:33:48");
		while (now.getTime() < limit.getTime()) {
			Date to = new Date(now.getTime() + (3600 * 1000));
			// Days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream("/total.csv")))) {
				List<SymbolStats> stats = BotUtils.loadPredictions(from, to, reader, false);
				trader.execute(stats);
				holder.execute(stats);
			}
			now = to;
			if (trader.isDidAction() && trader.getUsdtSnapshot() < holder.getUsdtSnapshot()) {
				System.out.println("WARNING: \n Trader: " + trader + "\n Holder: " + holder);
			}
		}
		System.out.println("Trader: " + trader);
		System.out.println("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() > holder.getUsdtSnapshot());
	}
	
	@Test
	public void round() {
		assertEquals("21330,888888", String.format("%.6f", 21330.888887878787));
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
