package com.jbescos.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.exchange.Broker;
import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.PublicAPI.Interval;
import com.jbescos.exchange.TransactionsSummary;
import com.jbescos.exchange.Utils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class UtilsTest {

    private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties(null);

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
        days = Utils.monthsBack(from, 2, "user/" + Utils.WALLET_PREFIX, ".csv");
        assertEquals(Arrays.asList("user/" + Utils.WALLET_PREFIX + "2019-12.csv", "user/" + Utils.WALLET_PREFIX + "2020-01.csv"), days);
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
        double minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "10", "1"),
                createCsvTransactionRow(Action.BUY, "15", "1"))).getMinProfitable();
        assertEquals("12.5", Utils.format(minSell));
        minSell = Utils
                .minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "10", "1"),
                        createCsvTransactionRow(Action.BUY, "15", "1"), createCsvTransactionRow(Action.BUY, "2", "1")))
                .getMinProfitable();
        assertEquals("9", Utils.format(minSell));
        minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "2", "4")))
                .getMinProfitable();
        assertEquals("0.5", Utils.format(minSell));
        minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "2", "4"),
                createCsvTransactionRow(Action.BUY, "1", "10"))).getMinProfitable();
        assertEquals("0.21428571", Utils.format(minSell));
        minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "4", "2"),
                createCsvTransactionRow(Action.SELL, "2", "1"))).getMinProfitable();
        assertEquals("2", Utils.format(minSell));
        minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "1", "4"),
                createCsvTransactionRow(Action.SELL, "1", "4"))).getMinProfitable();
        assertEquals("0", Utils.format(minSell));
        minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.SELL, "1", "10")))
                .getMinProfitable();
        assertEquals("0", Utils.format(minSell));
        double minSellExample = Utils
                .minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "14.0", "57.5"),
                        createCsvTransactionRow(Action.BUY, "10.0", "37.5")))
                .getMinProfitable();
        assertEquals("0.25263157", Utils.format(minSellExample));
        minSell = Utils.minSellProfitable(Arrays.asList(createCsvTransactionRow(Action.BUY, "14.0", "57.5"),
                createCsvTransactionRow(Action.BUY, "10.0", "37.5"),
                createCsvTransactionRow(Action.SELL, "14.0", "30.0"))).getMinProfitable();
        assertEquals("0.15384615", Utils.format(minSell));
        assertTrue(minSellExample > minSell);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sellWhenBenefitDifferentSymbols() {
        Utils.minSellProfitable(
                Arrays.asList(new CsvTransactionRow(new Date(0), "a", Action.BUY, "symbol1", "1.0", "2.0", 3.0),
                        new CsvTransactionRow(new Date(0), "a", Action.BUY, "symbol2", "1.0", "2.0", 3.0)));
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
    public void hourFormat() {
        assertEquals("13:30", Utils.fromDate(Utils.FORMAT_HOUR, Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 13:30:55")));
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
        assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:00:00"),
                Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:00:12")));
        assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:00:00"),
                Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:01:12")));
        assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:30:00"),
                Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:30:12")));
        assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:30:00"),
                Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:31:12")));
        assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:40:00"),
                Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:40:12")));
        assertEquals(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:40:00"),
                Utils.dateRoundedTo10Min(Utils.fromString(Utils.FORMAT_SECOND, "2021-07-10 00:49:12")));
    }

    @Test
    public void isPanicSellInDays() {
        Date dateLimit = Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:00:01");
        assertTrue(Utils.isPanicSellInDays(
                Arrays.asList(createCsvTransactionRow("2021-05-02 00:00:00", Action.SELL_PANIC, "1", "1")), dateLimit));
        assertFalse(Utils.isPanicSellInDays(
                Arrays.asList(createCsvTransactionRow("2021-05-01 00:00:00", Action.SELL_PANIC, "1", "1")), dateLimit));
        assertFalse(Utils.isPanicSellInDays(
                Arrays.asList(createCsvTransactionRow("2021-05-01 00:00:00", Action.SELL_PANIC, "1", "1"),
                        createCsvTransactionRow("2021-05-02 00:00:00", Action.BUY, "1", "1")),
                dateLimit));
        assertFalse(Utils
                .isPanicSellInDays(Arrays.asList(createCsvTransactionRow("2021-04-30 00:00:00", Action.BUY, "1", "1"),
                        createCsvTransactionRow("2021-05-01 00:00:00", Action.SELL_PANIC, "1", "1"),
                        createCsvTransactionRow("2021-05-02 00:00:00", Action.BUY, "1", "1")), dateLimit));
        assertTrue(
                Utils.isPanicSellInDays(
                        Arrays.asList(createCsvTransactionRow("2021-05-05 00:00:00", Action.BUY, "1", "1"),
                                createCsvTransactionRow("2021-05-05 00:00:01", Action.SELL_PANIC, "1", "1")),
                        dateLimit));
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
        double result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"),
                Utils.fromString(Utils.FORMAT_SECOND, "2021-05-02 00:46:01"), 0.08, -0.01, 0.2, 0);
        assertEquals(0.07, result, 0.001);
        result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"),
                Utils.fromString(Utils.FORMAT_SECOND, "2021-05-02 00:45:01"), 0.08, -0.01, 0.2, 0);
        assertEquals(0.08, result, 0.001);
        result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"),
                Utils.fromString(Utils.FORMAT_SECOND, "2022-05-02 00:45:01"), 0.08, -0.01, 0.2, 0);
        assertEquals(0.0, result, 0.001);
        result = Utils.minProfitSellAfterDays(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"),
                Utils.fromString(Utils.FORMAT_SECOND, "2022-05-02 00:45:01"), 0.08, 0.01, 0.2, 0);
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
        assertEquals("2021-04-30 22:46:01", Utils.fromDate(Utils.FORMAT_SECOND,
                Utils.getDateOfHoursBack(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), 2)));
        assertEquals("2021-05-06 00:46:01", Utils.fromDate(Utils.FORMAT_SECOND,
                Utils.getDateOfHoursBack(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-08 00:46:01"), 48)));
    }

    @Test
    public void getDateOfDaysBackZero() {
        assertEquals("2021-04-29 00:00:00", Utils.fromDate(Utils.FORMAT_SECOND,
                Utils.getDateOfDaysBackZero(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), 2)));
        assertEquals("2021-05-07 00:00:00", Utils.fromDate(Utils.FORMAT_SECOND,
                Utils.getDateOfDaysBackZero(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-08 00:46:01"), 1)));
    }

    @Test
    public void expectedCsvLine() {
        String expectedLine = "2021-06-18 14:05:14,,BUY,any,12.62573000,19.70000000,0.6409,0,false,0\r\n";
        CsvTransactionRow txRow = createCsvTransactionRow("2021-06-18 14:05:14", Action.BUY, "12.62573000",
                "19.70000000");
        assertEquals(expectedLine, txRow.toCsvLine());
    }

    @Test
    public void csvBenefitRow() {
        CsvTransactionRow sell = createCsvTransactionRow("2021-10-01 00:00:00", Action.SELL, "1000.8454", "1.3");
        CsvTransactionRow buy1 = createCsvTransactionRow("2021-08-01 00:00:00", Action.BUY, "500.6", "0.7");
        CsvTransactionRow buy2 = createCsvTransactionRow("2021-09-01 00:00:00", Action.BUY, "400", "0.6");
        TransactionsSummary summary = Utils.minSellProfitable(Arrays.asList(buy1, buy2));
        CsvProfitRow profitRow = CsvProfitRow.build("0.03", summary, sell);
        // SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%
        assertEquals(
                "2021-10-01 00:00:00,2021-08-01 00:00:00,any,1.3,1.3,900.6,1000.84,3%,3,100.24,97.23,11.13%,,,false"
                        + Utils.NEW_LINE,
                profitRow.toCsvLine());
        // Sell quantity does not match with but quantity because the user bought out of
        // the system. We only consider the bot proportion
        sell = createCsvTransactionRow("2021-10-01 00:00:00", Action.SELL, "1538", "2");
        profitRow = CsvProfitRow.build("0.03", summary, sell);
        // SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%
        assertEquals("2021-10-01 00:00:00,2021-08-01 00:00:00,any,1.3,1.3,900.6,999.7,3%,2.97,99.1,96.12,11%,,,false"
                + Utils.NEW_LINE, profitRow.toCsvLine());
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

    @Test
    public void sortBrokers() {
        Map<String, Broker> brokers = new HashMap<>();
        TestBroker a = new TestBroker(Action.SELL, 0.1, true);
        TestBroker b = new TestBroker(Action.SELL, 0.2, true);
        brokers.put("a", a);
        brokers.put("b", b);
        assertEquals(Arrays.asList(b, a), Utils.sortBrokers(brokers));
        brokers.clear();
        a = new TestBroker(Action.SELL, 0.1, true);
        b = new TestBroker(Action.SELL, 0.2, false);
        brokers.put("a", a);
        brokers.put("b", b);
        assertEquals(Arrays.asList(b, a), Utils.sortBrokers(brokers));
        a = new TestBroker(Action.SELL, 0.2, true);
        b = new TestBroker(Action.SELL, 0.1, false);
        brokers.put("a", a);
        brokers.put("b", b);
        assertEquals(Arrays.asList(b, a), Utils.sortBrokers(brokers));
        brokers.clear();
        a = new TestBroker(Action.SELL, 0.1, false);
        b = new TestBroker(Action.SELL, 0.2, false);
        brokers.put("a", a);
        brokers.put("b", b);
        assertEquals(Arrays.asList(b, a), Utils.sortBrokers(brokers));
        brokers.clear();
    }

    @Test
    public void dataToString() {
        CsvTransactionRow buy = new CsvTransactionRow(new Date(0), "1", Action.BUY, "test", "10", "10", 1);
        TransactionsSummary summary = new TransactionsSummary(true, 1, 1, new Date(0), Arrays.asList(buy),
                Collections.emptyList());
        CsvProfitRow profit = CsvProfitRow.build("0", summary,
                new CsvTransactionRow(new Date(0), "1", Action.SELL, "test", "10", "10", 1));
        assertEquals("test SELL 1970-01-01 01:00:00\n" + "First purchase: 1970-01-01 01:00:00\n"
                + "Buy / Sell: 10$ / 10$\n" + "Profit: 0$ (<b>0%</b>) ✅", profit.toString());
        profit = CsvProfitRow.build("0", summary,
                new CsvTransactionRow(new Date(0), "1", Action.SELL, "test", "9", "10", 1));
        assertEquals("test SELL 1970-01-01 01:00:00\n" + "First purchase: 1970-01-01 01:00:00\n"
                + "Buy / Sell: 10$ / 9$\n" + "Profit: -1$ (<b>-10%</b>) ❌", profit.toString());
        assertEquals("ORDER ID: 1\ntest BUY 1970-01-01 01:00:00\n" + "Total USD (USD per unit): 10$ (1$) 💵",
                buy.toString());
    }

    @Test
    public void isLowerPurchase() {
        assertFalse(Utils.isLowerPurchase(11, 10, 0.95));
        assertFalse(Utils.isLowerPurchase(10, 10, 0.95));
        assertFalse(Utils.isLowerPurchase(9.5, 10, 0.95));
        // If it is less than 97% of last purchase, then yes
        assertTrue(Utils.isLowerPurchase(9.4, 10, 0.95));
    }

    @Test
    public void calculatedUsdtCsvTransactionRow() {
        CsvTransactionRow tx = Utils.calculatedUsdtCsvTransactionRow(new Date(), "symbol", "orderId", Action.BUY, "10",
                5, 0.1);
        assertEquals("symbol", tx.getSymbol());
        assertEquals("orderId", tx.getOrderId());
        assertEquals(Action.BUY, tx.getSide());
        assertEquals("10", tx.getUsdt());
        assertEquals("1.8", tx.getQuantity());
        assertEquals(5, tx.getUsdtUnit(), 0.000001);
    }

    @Test
    public void calculatedSymbolCsvTransactionRow() {
        CsvTransactionRow tx = Utils.calculatedSymbolCsvTransactionRow(new Date(), "symbol", "orderId", Action.SELL,
                "10", 5, 0.1);
        assertEquals("symbol", tx.getSymbol());
        assertEquals("orderId", tx.getOrderId());
        assertEquals(Action.SELL, tx.getSide());
        assertEquals("10", tx.getQuantity());
        assertEquals("45", tx.getUsdt());
        assertEquals(5, tx.getUsdtUnit(), 0.000001);
    }

    @Test
    public void openPossitions2() {
        List<CsvTransactionRow> previousTx = Arrays.asList(
                Utils.calculatedUsdtCsvTransactionRow(new Date(10), "symbol1", "1", Action.BUY, "10", 5, 0.1),
                Utils.calculatedUsdtCsvTransactionRow(new Date(9), "symbol1", "2", Action.BUY, "10", 5, 0.1),
                Utils.calculatedUsdtCsvTransactionRow(new Date(8), "symbol2", "3", Action.BUY, "10", 5, 0.1),
                Utils.calculatedUsdtCsvTransactionRow(new Date(7), "symbol3", "4", Action.BUY, "10", 5, 0.1));
        List<CsvTransactionRow> newTx = Arrays.asList(
                Utils.calculatedUsdtCsvTransactionRow(new Date(0), "symbol1", "5", Action.SELL, "10", 5, 0.1),
                Utils.calculatedUsdtCsvTransactionRow(new Date(11), "symbol4", "6", Action.BUY, "10", 5, 0.1),
                Utils.calculatedUsdtCsvTransactionRow(new Date(12), "symbol3", "7", Action.BUY, "10", 5, 0.1));
        List<CsvTransactionRow> result = Utils.openPossitions2(
                previousTx.stream().collect(Collectors.groupingBy(CsvTransactionRow::getSymbol)), newTx);
        assertEquals(Arrays.asList("4", "3", "6", "7"),
                result.stream().map(CsvTransactionRow::getOrderId).collect(Collectors.toList()));
    }

    @Test
    public void bigVariationFearGreedIndex() {
        assertEquals(true, Utils.bigVariationFearGreedIndex(100, 10));
        assertEquals(true, Utils.bigVariationFearGreedIndex(50, 10));
        assertEquals(false, Utils.bigVariationFearGreedIndex(50, 50));
        assertEquals(false, Utils.bigVariationFearGreedIndex(51, 50));
        assertEquals(true, Utils.bigVariationFearGreedIndex(20, 10));
        assertEquals(true, Utils.bigVariationFearGreedIndex(16, 12));
    }

    @Test
    public void resyncProfit() throws IOException {
        List<CsvTransactionRow> transactions = null;
        List<CsvProfitRow> profits = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/resync/zzzDaviX_transactions_transactions_2022-06.csv")))) {
            transactions = CsvUtil.readCsvTransactionRows(true, ",", reader);
        }
        assertNotNull(transactions);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("/resync/zzzDaviX_profit_profit_2022-06.csv")))) {
            profits = CsvUtil.readCsvProfitRows(reader);
        }
        assertNotNull(profits);
        Map<String, CsvTransactionRow> byOrderId = new HashMap<>();
        transactions.stream().forEach(tx -> byOrderId.put(tx.getOrderId(), tx));
        assertEquals("-2.01%", profits.get(0).getProfitPercentage());
        assertEquals("-2.18%", profits.get(1).getProfitPercentage());
        assertEquals("8.39%", profits.get(2).getProfitPercentage());
        assertTrue(Utils.resyncProfit(byOrderId, profits, "0.15"));
        assertEquals("15%", profits.get(0).getCommissionPercentage());
        assertEquals("-2.5%", profits.get(0).getProfitPercentage());
        assertEquals("-2.3%", profits.get(1).getProfitPercentage());
        assertEquals("6.2%", profits.get(2).getProfitPercentage());
    }
    
    @Test
    public void delayto30or00() {
        assertEquals(300000, Utils.delayto30or00(Utils.fromString(Utils.FORMAT_SECOND, "2022-12-08 23:55:00")));
        assertEquals(1500000, Utils.delayto30or00(Utils.fromString(Utils.FORMAT_SECOND, "2022-12-08 23:05:00")));
        assertEquals(1800000, Utils.delayto30or00(Utils.fromString(Utils.FORMAT_SECOND, "2022-12-08 23:00:00")));
        assertEquals(60000, Utils.delayto30or00(Utils.fromString(Utils.FORMAT_SECOND, "2022-12-08 22:59:59")));
        assertEquals(1800000, Utils.delayto30or00(Utils.fromString(Utils.FORMAT_SECOND, "2022-12-08 23:30:00")));
        assertEquals(60000, Utils.delayto30or00(Utils.fromString(Utils.FORMAT_SECOND, "2022-12-08 23:29:59")));
    }

    @Test
    public void splitSize() {
        assertEquals(Arrays.asList("this ", "is a ", "test.", ".."), Utils.splitSize("this is a test...", 5));
        assertEquals(Arrays.asList(), Utils.splitSize("", 5));
        assertEquals(Arrays.asList("a"), Utils.splitSize("a", 5));
    }
    
    @Test
    public void lastModified() {
        assertEquals(Arrays.asList("c","c","c"), Utils.lastModified(Arrays.asList("a","a","a","b","c","c","c"), s -> s));
        assertEquals(Arrays.asList(), Utils.lastModified(Arrays.asList(), s -> s));
        assertEquals(Arrays.asList("c"), Utils.lastModified(Arrays.asList("a","a","a","b","c"), s -> s));
        assertEquals(Arrays.asList("c"), Utils.lastModified(Arrays.asList("c"), s -> s));
    }

    @Test
    public void signEIP712() throws IOException {
        String privateKey = "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6";
        String expectedSignature = "0x36ac2b5f0a1c91e275684538fce2b398b214ece08fac7ccb831c0eef03303d6118657d09b615097f944fdb41bcb950046b720c2b238928af423f4e69aaaad36c1b";
        try (InputStream in = Utils.class.getResourceAsStream("/eip712.json")) {
            JsonReader reader = Json.createReader(in);
            JsonObject main = reader.readObject();
            JsonObject data = main.getJsonObject("data");
            JsonObject eip712Data = data.getJsonObject("eip712Data");
            String signature = Utils.signEIP712(eip712Data.toString(), privateKey);
            assertEquals(expectedSignature, signature);
        }
    }
    
    @Test
    public void signEIP712_2() throws IOException {
        String privateKey = "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6";
        String expectedSignature = "0xb22299b76b1e544f8a5613fc58ad9d02ddf31ed3e9b0ccf0ec6a27930a9b90c5437f1316a440ca1f3c7cab0d466b24c51a1a4751210e4d55e644c7b91c357e1d1b";
        try (InputStream in = Utils.class.getResourceAsStream("/eip712_2.json")) {
            JsonReader reader = Json.createReader(in);
            JsonObject main = reader.readObject();
            JsonObject data = main.getJsonObject("data");
            JsonObject eip712Data = data.getJsonObject("eip712Data");
            String signature = Utils.signEIP712(eip712Data.toString(), privateKey);
            assertEquals(expectedSignature, signature);
        }
    }
    
    private Map<String, String> createWalletRow(String symbol, String usdt) {
        Map<String, String> row = new HashMap<>();
        row.put("SYMBOL", symbol);
        row.put(Utils.USDT, usdt);
        return row;
    }

    private CsvTransactionRow createCsvTransactionRow(Action side, String usdt, String quantity) {
        return createCsvTransactionRow("2021-01-01 00:00:00", side, usdt, quantity);
    }

    private CsvTransactionRow createCsvTransactionRow(String date, Action side, String usdt, String quantity) {
        double result = Double.parseDouble(usdt) / Double.parseDouble(quantity);
        return new CsvTransactionRow(Utils.fromString(Utils.FORMAT_SECOND, date), "", side, "any", usdt, quantity,
                result);
    }

    private static final class TestBroker implements Broker {

        private Action action;
        private final double factor;
        private final boolean hasPreviousTransactions;

        private TestBroker(Action action, double factor, boolean hasPreviousTransactions) {
            this.action = action;
            this.factor = factor;
            this.hasPreviousTransactions = hasPreviousTransactions;
        }

        @Override
        public Action getAction() {
            return action;
        }

        @Override
        public CsvRow getNewest() {
            return null;
        }

        @Override
        public String getSymbol() {
            return null;
        }

        @Override
        public double getFactor() {
            return factor;
        }

        @Override
        public TransactionsSummary getPreviousTransactions() {
            return new TransactionsSummary(hasPreviousTransactions, 0, 0, null, null, null);
        }

        @Override
        public void evaluate(double benefitsAvg) {
        }

        @Override
        public List<CsvRow> getValues() {
            return null;
        }

        @Override
        public void setAction(Action action) {
            this.action = action;
        }

    }
}
