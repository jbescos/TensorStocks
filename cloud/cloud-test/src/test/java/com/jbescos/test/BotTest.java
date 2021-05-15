package com.jbescos.test;

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
		Map<String, Double> wallet = new HashMap<>();
		wallet.put(Utils.USDT, 500.0);
		Bot trader = new Bot(wallet, false);
		Bot holder = new Bot(new HashMap<>(wallet), true);
		Date limit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-15 15:33:48");
		Date now = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-11 08:33:48");
		while (now.getTime() < limit.getTime()) {
			Date to = new Date(now.getTime() + (3600 * 1000));
			// 3 days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream("/simple.csv")))) {
				List<SymbolStats> stats = BotUtils.loadPredictions(from, to, reader);
				trader.execute(stats);
				holder.execute(stats);
			}
			now = to;
		}
		System.out.println("Trader: " + trader);
		System.out.println("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() > holder.getUsdtSnapshot());
	}
	
	@Test
	public void complex() throws FileNotFoundException, IOException {
		Map<String, Double> wallet = new HashMap<>();
		wallet.put(Utils.USDT, 500.0);
		Bot trader = new Bot(wallet, false, Arrays.asList("DOGE", "DOT", "BTT"));
		Bot holder = new Bot(new HashMap<>(wallet), true);
		Date limit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-15 06:00:08");
		Date now = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-13 08:33:48");
		while (now.getTime() < limit.getTime()) {
			Date to = new Date(now.getTime() + (3600 * 1000));
			// 3 days back
			Date from = new Date(now.getTime() - (DAYS_BACK_MILLIS));
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream("/total.csv")))) {
				List<SymbolStats> stats = BotUtils.loadPredictions(from, to, reader);
				trader.execute(stats);
				holder.execute(stats);
			}
			now = to;
		}
		System.out.println("Trader: " + trader);
		System.out.println("Holder: " + holder);
		assertTrue("Trader: " + trader + " \n " + "Holder: " + holder + "\n", trader.getUsdtSnapshot() > holder.getUsdtSnapshot());
	}
}
