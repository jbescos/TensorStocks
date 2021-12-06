package com.jbescos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.jbescos.common.Broker.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvProfitRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.PublicAPI.Interval;
import com.jbescos.common.TransactionsSummary;
import com.jbescos.common.Utils;

public class UtilsTest {

	private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties();

	@Test
	public void daysBack() {
		Date from = Utils.fromString(Utils.FORMAT, "2020-01-02");
		List<String> days = Utils.daysBack(from, 4, "", ".csv");
		assertEquals(Arrays.asList("2019-12-30.csv", "2019-12-31.csv", "2020-01-01.csv", "2020-01-02.csv"), days);
	}
	
    @Test
    public void monthsBack() {
        Date from = Utils.fromString(Utils.FORMAT, "2020-01-02");
        List<String> days = Utils.monthsBack(from, 4, "", ".csv");
        assertEquals(Arrays.asList("2019-10.csv", "2019-11.csv", "2019-12.csv", "2020-01.csv"), days);
    }
	
	@Test
	public void minSell() {
		double value = CLOUD_PROPERTIES.minSell("does not exist");
		assertEquals(0.0, value, 0.0);
		value = CLOUD_PROPERTIES.minSell("TESTUSDT");
		assertEquals(101, value, 0.0);
	}
	
	
	@Test
	public void sellWhenBenefit() {
		// 1 buy = 10$, 1 buy = 15$ -> 1 sell = 12.5
		double minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "10", "1"), createCsvTransactionRow(Action.BUY, "15", "1"))).getMinProfitable();
		assertEquals("12.5", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "10", "1"), createCsvTransactionRow(Action.BUY, "15", "1"), createCsvTransactionRow(Action.BUY, "2", "1"))).getMinProfitable();
		assertEquals("9", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "2", "4"))).getMinProfitable();
		assertEquals("0.5", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "2", "4"), createCsvTransactionRow(Action.BUY, "1", "10"))).getMinProfitable();
		assertEquals("0.21428571", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "4", "2"), createCsvTransactionRow(Action.SELL, "2", "1"))).getMinProfitable();
		assertEquals("2", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "1", "4"), createCsvTransactionRow(Action.SELL, "1", "4"))).getMinProfitable();
		assertEquals("0", Utils.format(minSell));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.SELL, "1", "10"))).getMinProfitable();
		assertEquals("0", Utils.format(minSell));
		double minSellExample = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "14.0", "57.5"), createCsvTransactionRow(Action.BUY, "10.0", "37.5"))).getMinProfitable();
		assertEquals("0.25263157", Utils.format(minSellExample));
		minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "14.0", "57.5"), createCsvTransactionRow(Action.BUY, "10.0", "37.5"), createCsvTransactionRow(Action.SELL, "14.0", "30.0"))).getMinProfitable();
		assertEquals("0.15384615", Utils.format(minSell));
		assertTrue(minSellExample > minSell);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void sellWhenBenefitDifferentSymbols() {
		Utils.minSellProfitable(Arrays.asList(new CsvTransactionRow(new Date(0), "a", Action.BUY, "symbol1", "1.0", "2.0", 3.0), new CsvTransactionRow(new Date(0), "a", Action.BUY, "symbol2", "1.0", "2.0", 3.0)));
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
		String quantity = Utils.filterLotSizeQuantity("388.8108", "0.1", "9000000", "0.1");
        assertEquals("388.8", quantity);
	    quantity = Utils.filterLotSizeQuantity("2.254", "1.00", "10000000000.00", "0.02");
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
        quantity = Utils.filterLotSizeQuantity("9.30", "0.01", "900000", "0.01");
        assertEquals("9.3", quantity);
        quantity = Utils.filterLotSizeQuantity("9.29999999", "0.01", "900000", "0.01");
        assertEquals("9.29", quantity);
	}
	
	@Test
	public void sortForChart() {
		List<String> symbols = new ArrayList<>(Arrays.asList("SYMBOL1", "SYMBOL2", "BUY-SYMBOL", "SELL-SYMBOL"));
		Utils.sortForChart(symbols);
		assertEquals(Arrays.asList("SELL-SYMBOL", "BUY-SYMBOL", "SYMBOL1", "SYMBOL2"), symbols);
	}
	
	@Test
	public void dateRoundedTo10Min() {
		assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:00:00"), Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:00:12")));
		assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:00:00"), Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:01:12")));
		assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:30:00"), Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:30:12")));
		assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:30:00"), Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:31:12")));
		assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:40:00"), Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:40:12")));
		assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:40:00"), Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:49:12")));
	}
	
	@Test
	public void isPanicSellInDays() {
	    Date dateLimit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:00:01");
	    assertTrue(Utils.isPanicSellInDays(Arrays.asList(createCsvTransactionRow("2021-05-02 00:00:00", Action.SELL_PANIC, "1", "1")), dateLimit));
	    assertFalse(Utils.isPanicSellInDays(Arrays.asList(createCsvTransactionRow("2021-05-01 00:00:00", Action.SELL_PANIC, "1", "1")), dateLimit));
	    assertFalse(Utils.isPanicSellInDays(Arrays.asList(
	            createCsvTransactionRow("2021-05-01 00:00:00", Action.SELL_PANIC, "1", "1"),
	            createCsvTransactionRow("2021-05-02 00:00:00", Action.BUY, "1", "1"))
	            , dateLimit));
	    assertFalse(Utils.isPanicSellInDays(Arrays.asList(
	            createCsvTransactionRow("2021-04-30 00:00:00", Action.BUY, "1", "1"),
                createCsvTransactionRow("2021-05-01 00:00:00", Action.SELL_PANIC, "1", "1"),
                createCsvTransactionRow("2021-05-02 00:00:00", Action.BUY, "1", "1"))
                , dateLimit));
	    assertTrue(Utils.isPanicSellInDays(Arrays.asList(
                createCsvTransactionRow("2021-05-05 00:00:00", Action.BUY, "1", "1"),
                createCsvTransactionRow("2021-05-05 00:00:01", Action.SELL_PANIC, "1", "1"))
                , dateLimit));
	}
	
	@Test
	public void inPercentile() {
	    assertTrue(Utils.inPercentile(0.9, 0.95, 0, 1));
	    assertFalse(Utils.inPercentile(0.9, 0.89, 0, 1));
	    assertTrue(Utils.inPercentile(0.9, 1.95, 1, 2));
	    assertFalse(Utils.inPercentile(0.9, 1.89, 1, 2));
	    
	    assertTrue(Utils.inPercentile(0.1, 0.11, 0, 1));
        assertFalse(Utils.inPercentile(0.1, 0.09, 0, 1));
        assertTrue(Utils.inPercentile(0.1, 1.11, 1, 2));
        assertFalse(Utils.inPercentile(0.1, 1.09, 1, 2));
	}
	
	@Test
	public void intervals() {
		Date d1 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:00:01");
		Date d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:00:01");
		assertEquals(Interval.MINUTES_1, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:01:01");
		assertEquals(Interval.MINUTES_1, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:03:01");
		assertEquals(Interval.MINUTES_3, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:05:01");
		assertEquals(Interval.MINUTES_5, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:15:01");
		assertEquals(Interval.MINUTES_15, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:30:01");
		assertEquals(Interval.MINUTES_30, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 01:00:01");
		assertEquals(Interval.HOUR_1, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:31:01");
		assertEquals(Interval.MINUTES_30, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:41:01");
		assertEquals(Interval.MINUTES_30, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01");
		assertEquals(Interval.HOUR_1, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:45:01");
		assertEquals(Interval.HOUR_1, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:45:00");
		assertEquals(Interval.MINUTES_30, Interval.getInterval(d1.getTime(), d2.getTime()));
		d2 = Utils.fromString(Utils.FORMAT_SECOND, "2030-05-01 00:00:01");
		assertEquals(Interval.MONTH_1, Interval.getInterval(d1.getTime(), d2.getTime()));
	}
	
	@Test
	public void minProfitSellAfterDays() {
	    double result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), Utils.fromString(Utils.FORMAT_SECOND, "2021-05-02 00:46:01"), 0.08, -0.01, 0.2, 0);
	    assertEquals(0.07, result, 0.001);
	    result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), Utils.fromString(Utils.FORMAT_SECOND, "2021-05-02 00:45:01"), 0.08, -0.01, 0.2, 0);
        assertEquals(0.08, result, 0.001);
        result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), Utils.fromString(Utils.FORMAT_SECOND, "2022-05-02 00:45:01"), 0.08, -0.01, 0.2, 0);
        assertEquals(0.0, result, 0.001);
        result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), Utils.fromString(Utils.FORMAT_SECOND, "2022-05-02 00:45:01"), 0.08, 0.01, 0.2, 0);
        assertEquals(0.2, result, 0.001);
	}
	
	@Test
	public void symbolValue() {
		assertEquals("2.42730229", Utils.format(Utils.symbolValue(1000, 411.98)));
	}

	@Test
	public void usdValue() {
		assertEquals("823.96", Utils.format(Utils.usdValue(2, 411.98)));
	}
	
	@Test
	public void getHoursOfDaysBack() {
		assertEquals("2021-04-30 22:46:01", Utils.fromDate(Utils.FORMAT_SECOND, Utils.getHoursOfDaysBack(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), 2)));
		assertEquals("2021-05-06 00:46:01", Utils.fromDate(Utils.FORMAT_SECOND, Utils.getHoursOfDaysBack(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-08 00:46:01"), 48)));
	}

	@Test
	public void expectedCsvLine() {
	    String expectedLine = "2021-06-18 14:05:14,,BUY,any,12.62573000,19.70000000,0.6409\r\n";
	    CsvTransactionRow txRow = createCsvTransactionRow("2021-06-18 14:05:14", Action.BUY, "12.62573000", "19.70000000");
	    assertEquals(expectedLine, txRow.toCsvLine());
	}
	
	@Test
	public void csvBenefitRow() {
		CsvTransactionRow sell = createCsvTransactionRow("2021-10-01 00:00:00", Action.SELL, "1000.8454", "1.3");
		CsvTransactionRow buy1 = createCsvTransactionRow("2021-08-01 00:00:00", Action.BUY, "500.6", "0.7");
		CsvTransactionRow buy2 = createCsvTransactionRow("2021-09-01 00:00:00", Action.BUY, "400", "0.6");
		TransactionsSummary summary = Utils.minSellProfitable(Arrays.asList(buy2, buy1));
		CsvProfitRow profitRow = CsvProfitRow.build("0.03", summary, sell);
		// SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%
		assertEquals("2021-10-01 00:00:00,2021-08-01 00:00:00,any,1.3,1.3,900.6,1000.8454,3%,3.007362,100.2454,97.238038,11.13095713%" + Utils.NEW_LINE, profitRow.toCsvLine());
		// Sell quantity does not match with but quantity because the user bought out of the system. We only consider the bot proportion
		sell = createCsvTransactionRow("2021-10-01 00:00:00", Action.SELL, "1538", "2");
		profitRow = CsvProfitRow.build("0.03", summary, sell);
		// SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%
		assertEquals("2021-10-01 00:00:00,2021-08-01 00:00:00,any,1.3,1.3,900.6,999.7,3%,2.973,99.1,96.127,11.00377526%" + Utils.NEW_LINE, profitRow.toCsvLine());
	}
	
	@Test
	public void walletInSymbolUsdt() {
		Map<String, Double> prices = new LinkedHashMap<>();
		prices.put("BTCUSDT", 49076.53);
		prices.put("ETHUSDT", 4172.53);
		prices.put("FAKEUSDT", 9999999.9);
		Map<String, String> wallet = new LinkedHashMap<>();
		wallet.put("BTC", "2");
		wallet.put("ETH", "1");
		wallet.put(Utils.USDT, "10");
		Map<String, String> walletInUsdt = Utils.walletInSymbolUsdt(prices, wallet);
		assertEquals("98153.06", walletInUsdt.get("BTCUSDT"));
		assertEquals("4172.53", walletInUsdt.get("ETHUSDT"));
		assertEquals("10", walletInUsdt.get(Utils.USDT));
		assertEquals("102335.59", walletInUsdt.get(Utils.TOTAL_USDT));
		String date = "2021-06-18 14:05:14";
		List<Map<String, String>> rows = Utils.userUsdt(Utils.fromString(Utils.FORMAT_SECOND, date), prices, wallet);
		Map<String, String> userWallet = rows.get(0);
		assertEquals(date, userWallet.get("DATE"));
		assertEquals("BTC", userWallet.get("SYMBOL"));
		assertEquals("2", userWallet.get("SYMBOL_VALUE"));
		assertEquals("98153.06", userWallet.get(Utils.USDT));
		userWallet = rows.get(1);
		assertEquals(date, userWallet.get("DATE"));
		assertEquals("ETH", userWallet.get("SYMBOL"));
		assertEquals("1", userWallet.get("SYMBOL_VALUE"));
		assertEquals("4172.53", userWallet.get(Utils.USDT));
		userWallet = rows.get(2);
		assertEquals(date, userWallet.get("DATE"));
		assertEquals(Utils.USDT, userWallet.get("SYMBOL"));
		assertEquals("10", userWallet.get("SYMBOL_VALUE"));
		assertEquals("10", userWallet.get(Utils.USDT));
		userWallet = rows.get(3);
		assertEquals(date, userWallet.get("DATE"));
		assertEquals(Utils.TOTAL_USDT, userWallet.get("SYMBOL"));
		assertEquals("102335.59", userWallet.get("SYMBOL_VALUE"));
		assertEquals("102335.59", userWallet.get(Utils.USDT));
	}

	private CsvTransactionRow createCsvTransactionRow(Action side, String usdt, String quantity) {
		return createCsvTransactionRow("2021-01-01 00:00:00", side, usdt, quantity);
	}
	
    private CsvTransactionRow createCsvTransactionRow(String date, Action side, String usdt, String quantity) {
        double result = Double.parseDouble(usdt) / Double.parseDouble(quantity);
        return new CsvTransactionRow(Utils.fromString(Utils.FORMAT_SECOND, date), "", side, "any", usdt, quantity, result);
    }
}
