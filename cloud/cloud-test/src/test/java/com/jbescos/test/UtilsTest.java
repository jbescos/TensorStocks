package com.jbescos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.Utils;

public class UtilsTest {

	@Test
	public void daysBack() {
		Date from = Utils.fromString(Utils.FORMAT, "2020-01-02");
		List<String> days = Utils.daysBack(from, 4, "", ".csv");
		assertEquals(Arrays.asList("2019-12-30.csv", "2019-12-31.csv", "2020-01-01.csv", "2020-01-02.csv"), days);
	}
	
	@Test
	public void minSell() {
		double value = CloudProperties.minSell("does not exist");
		assertEquals(0.0, value, 0.0);
		value = CloudProperties.minSell("TESTUSDT");
		assertEquals(101, value, 0.0);
	}
	
	
	@Test
	public void sellWhenBenefit() {
		// 1 buy = 10$, 1 buy = 15$ -> 1 sell = 12.5
		double minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 10, 1), createCsvTransactionRow(Action.BUY, 15, 1)));
		assertEquals("12.5", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 10, 1), createCsvTransactionRow(Action.BUY, 15, 1), createCsvTransactionRow(Action.BUY, 2, 1)));
		assertEquals("9", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 2, 4)));
		assertEquals("0.5", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 2, 4), createCsvTransactionRow(Action.BUY, 1, 10)));
		assertEquals("0.21428571", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 4, 2), createCsvTransactionRow(Action.SELL, 2, 1)));
		assertEquals("2", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 1, 4), createCsvTransactionRow(Action.SELL, 1, 4)));
		assertEquals("0", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.SELL, 1, 10)));
		assertEquals("0", Utils.format(minSell));
		double minSellExample = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 14.0, 57.5), createCsvTransactionRow(Action.BUY, 10.0, 37.5)));
		assertEquals("0.25263157", Utils.format(minSellExample));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, 14.0, 57.5), createCsvTransactionRow(Action.BUY, 10.0, 37.5), createCsvTransactionRow(Action.SELL, 14.0, 30.0)));
		assertEquals("0.15384615", Utils.format(minSell));
		assertTrue(minSellExample > minSell);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void sellWhenBenefitDifferentSymbols() {
		Utils.minSellProfitable(Arrays.asList(new CsvTransactionRow(new Date(0), "a", Action.BUY, "symbol1", 1.0, 2.0, 3.0), new CsvTransactionRow(new Date(0), "a", Action.BUY, "symbol2", 1.0, 2.0, 3.0)));
	}
	
	@Test
	public void ewma() {
		final double PRECISSION = 0.01;
		final double CONSTANT = 0.1;
		double result = Utils.ewma(CONSTANT, 1, null);
		assertEquals(1, result, PRECISSION);
		result = Utils.ewma(CONSTANT, 2, result);
		assertEquals(1.1, result, PRECISSION);
		result = Utils.ewma(CONSTANT, 5, result);
		assertEquals(1.49, result, PRECISSION);
		result = Utils.ewma(CONSTANT, 4, result);
		assertEquals(1.74, result, PRECISSION);
		result = Utils.ewma(CONSTANT, 3, result);
		assertEquals(1.87, result, PRECISSION);
	}
	
	@Test
	public void ewma2() {
//		2021-05-08 08:33:48,BTCUSDT,59299.98,59299.98
//		2021-05-08 08:34:50,BTCUSDT,59276.18,59298.79
//		2021-05-08 09:00:04,BTCUSDT,58899.99,59278.85
		final double PRECISSION = 0.01;
		final double CONSTANT = 0.05;
		double result = Utils.ewma(CONSTANT, 59299.98, 59299.98);
		assertEquals(59299.98, result, PRECISSION);
		result = Utils.ewma(CONSTANT, 59276.18, 59299.98);
		assertEquals(59298.79, result, PRECISSION);
		result = Utils.ewma(CONSTANT, 58899.99, 59298.79);
		assertEquals(59278.85, result, PRECISSION);
	}
	
	@Test
	public void doubleformat() {
		double value = 36601014.83;
		assertEquals("36601014.83", Utils.format(value));
		value = 36601014.8300000000;
		assertEquals("36601014.83", Utils.format(value));
		value = 36604541014.83;
		assertEquals("36604541014.83", Utils.format(value));
		value = 0.83;
		assertEquals("0.83", Utils.format(value));
		value = 0.99999999;
		assertEquals("0.99999999", Utils.format(value));
		value = 0.999999999;
		assertEquals("0.99999999", Utils.format(value));
	}
	
	@Test
	public void filterLotSizeQuantity() {
	    String quantity = Utils.filterLotSizeQuantity("2.254", "1.00", "10000000000.00", "0.02");
        assertEquals("2.24", quantity);
        quantity = Utils.filterLotSizeQuantity("291.2", "0.10000000", "9222449.00000000", "0.10000000");
        assertEquals("291.2", quantity);
        quantity = Utils.filterLotSizeQuantity("0.06178180", "0.00010000", "900000.00000000", "0.00010000");
        assertEquals("0.0617", quantity);
	    quantity = Utils.filterLotSizeQuantity("36601014.83", "1.00", "10000000000.00", "1.00");
	    assertEquals("36601014", quantity);
	    quantity = Utils.filterLotSizeQuantity("0.5", "1.00", "10000000000.00", "1.00");
	    assertEquals("1", quantity);
	    quantity = Utils.filterLotSizeQuantity("10000000001.00", "1.00", "10000000000.00", "1.00");
        assertEquals("10000000000", quantity);
	}
	
	@Test
	public void sortForChart() {
		List<String> symbols = new ArrayList<>(Arrays.asList("SYMBOL1", "SYMBOL2", "BUY-SYMBOL", "SELL-SYMBOL"));
		Utils.sortForChart(symbols);
		assertEquals(Arrays.asList("SELL-SYMBOL", "BUY-SYMBOL", "SYMBOL1", "SYMBOL2"), symbols);
	}
	
	private CsvTransactionRow createCsvTransactionRow(Action side, double usdt, double quantity) {
		return new CsvTransactionRow(new Date(0), "", side, "any", usdt, quantity, usdt / quantity);
	}
}
