package com.jbescos.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.SecuredMizarAPI.ClosePositionResponse;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    public static final long MINUTES_30_MILLIS = 30 * 60 * 1000;
    public static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
    public static final double LOWER_PURCHASE_REDUCER = 0.97;
    public static final double LOWER_LIMITS_PURCHASE_REDUCER = 0.90;
    public static final String FORMAT_MONTH = "yyyy-MM";
    public static final String FORMAT = "yyyy-MM-dd";
    public static final String FORMAT_SECOND = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_HOUR = "HH:mm";
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final String USDT = "USDT";
    public static final String TOTAL_USDT = "TOTAL_USDT";
    public static final String NEW_LINE = "\r\n";
    public static final String CSV_ROW_HEADER = "DATE,SYMBOL,PRICE,AVG,AVG_2,FEAR_GREED_IDX,FEAR_GREED_IDX_AVG,TOKEN"
            + NEW_LINE;
    public static final String TX_ROW_HEADER = "DATE,ORDER_ID,SIDE,SYMBOL,USDT,QUANTITY,USDT_UNIT,SCORE,SYNC,FEE"
            + NEW_LINE;
    public static final String KLINE_ROW_HEADER = "OPEN_TIME,CLOSE_TIME,SYMBOL,HIGH,LOW,OPEN,CLOSE,VOLUME,ASSET_VOLUME,SUPPORT_LIST,RESISTANCE_LIST"
            + NEW_LINE;
    public static final String LAST_PRICE = "last_price.csv";
    public static final String EMPTY_STR = "";
    public static final String TRANSACTIONS_PREFIX = "transactions/transactions_";
    public static final String CONTEXT_PREFIX = "context/";
    public static final String OPEN_POSSITIONS = CONTEXT_PREFIX + "open_positions.csv";
    public static final String CONTEXT_DATA_FILE = CONTEXT_PREFIX + "contextData.csv";
    public static final String WALLET_PREFIX = "wallet/wallet_";
    public static final String TX_SUMMARY_PREFIX = "tx_summary/tx_summary_";
    public static final double MIN_WALLET_VALUE_TO_RECORD = 9.5;
    public static final double EWMA_CONSTANT = 0.01;
    public static final double EWMA_2_CONSTANT = 0.001;
    public static final double TRANSFER_MIN_PROFIT_UNIT = 0.03;
    public static final String BENEFITS_AVG = "AVG";
    public static final String ORDER_ID_SPLIT = "-";

    public static Properties fromClasspath(String properties) throws IOException {
        try (InputStream in = Utils.class.getResourceAsStream(properties)) {
            if (in != null) {
                Properties prop = new Properties();
                prop.load(in);
                return prop;
            }
        }
        return null;
    }

    private static List<String> dateBack(Date currentTime, int unitBack, String prefix, String subfix, int dateType,
            String dateFormat) {
        List<String> days = new ArrayList<>(unitBack);
        Calendar c = Calendar.getInstance();
        c.setTime(currentTime);
        c.add(dateType, unitBack * -1);
        for (int i = 0; i < unitBack; i++) {
            c.add(dateType, 1);
            days.add(prefix + fromDate(dateFormat, c.getTime()) + subfix);
        }
        return days;
    }

    public static List<String> daysBack(Date currentTime, int daysBack, String prefix, String subfix) {
        return dateBack(currentTime, daysBack, prefix, subfix, Calendar.DAY_OF_YEAR, FORMAT);
    }

    public static List<String> monthsBack(Date currentTime, int monthsBack, String prefix, String subfix) {
        return dateBack(currentTime, monthsBack, prefix, subfix, Calendar.MONTH, FORMAT_MONTH);
    }

    public static Date getDateOfDaysBack(Date currentTime, int daysBack) {
        Calendar c = Calendar.getInstance();
        c.setTime(currentTime);
        c.add(Calendar.DAY_OF_YEAR, daysBack * -1);
        return c.getTime();
    }

    public static Date getDateOfDaysBackZero(Date currentTime, int daysBack) {
        Calendar c = Calendar.getInstance();
        c.setTime(currentTime);
        c.add(Calendar.DAY_OF_YEAR, daysBack * -1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date getDateOfHoursBack(Date currentTime, int hoursBack) {
        Calendar c = Calendar.getInstance();
        c.setTime(currentTime);
        c.add(Calendar.HOUR, hoursBack * -1);
        return c.getTime();
    }

    public static Date fromString(String format, String date) {
        return fromString(new SimpleDateFormat(format), date);
    }

    public static Date fromString(DateFormat format, String date) {
        try {
            return format.parse(date);
        } catch (ParseException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Cannot parse " + date + " with format " + format, e);
        }
    }

    public static String fromDate(String format, Date date) {
        return new SimpleDateFormat(format).format(date);
    }

    public static String getParam(String param, String defaultValue, Map<String, List<String>> parameters) {
        List<String> values = parameters.get(param);
        if (values == null || values.isEmpty()) {
            return defaultValue;
        } else {
            return values.get(0);
        }
    }

    public static String todayWithSeconds() {
        return fromDate(FORMAT_SECOND, new Date());
    }

    public static String today() {
        return fromDate(FORMAT, new Date());
    }

    public static String thisMonth() {
        return fromDate(FORMAT_MONTH, new Date());
    }

    public static String thisMonth(Date date) {
        return fromDate(FORMAT_MONTH, date);
    }

    public static String format(double amount) {
        return format(amount, 8);
    }

    public static String format(double amount, int digits) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0", symbols);
        df.setRoundingMode(RoundingMode.DOWN);
        df.setMaximumFractionDigits(digits);
        return df.format(amount);
    }

    public static String format(BigDecimal amount, int digits) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("0", symbols);
        df.setRoundingMode(RoundingMode.DOWN);
        df.setMaximumFractionDigits(digits);
        return df.format(amount);
    }

    public static String format(BigDecimal amount) {
        return format(amount, 8);
    }

    public static TransactionsSummary minSellProfitable(List<CsvTransactionRow> previousTransactions) {
        Date lastPurchase = null;
        List<CsvTransactionRow> buys = new ArrayList<>();
        List<CsvTransactionRow> sells = new ArrayList<>();
        boolean hasTransactions = false;
        double minProfitable = 0;
        double lowestPurchase = Double.MAX_VALUE;
        if (previousTransactions == null || previousTransactions.isEmpty()) {
            minProfitable = 0;
        } else {
            double accumulated = 0.0;
            double totalQuantity = 0.0;
            String symbol = null;
            lastPurchase = previousTransactions.stream().filter(tx -> tx.getSide() == Action.BUY)
                    .map(tx -> tx.getDate()).sorted((d1, d2) -> d2.compareTo(d1)).findFirst().orElse(null);
            for (CsvTransactionRow transaction : previousTransactions) {
                if (symbol == null) {
                    symbol = transaction.getSymbol();
                } else if (!symbol.equals(transaction.getSymbol())) {
                    throw new IllegalArgumentException("Every CsvAccountRow must contain the same symbol. It was found "
                            + symbol + " and " + transaction.getSymbol());
                }
                try {
                    double quantity = Double.parseDouble(transaction.getQuantity());
                    double usdt = Double.parseDouble(transaction.getUsdt());
                    if (transaction.getSide() == Action.BUY) {
                        double usdtUnit = usdt / quantity;
                        if (usdtUnit < lowestPurchase) {
                            lowestPurchase = usdtUnit;
                        }
                        totalQuantity = totalQuantity + quantity;
                        accumulated = accumulated + usdt;
                        buys.add(transaction);
                    } else {
                        accumulated = accumulated - usdt;
                        totalQuantity = totalQuantity - quantity;
                        sells.add(transaction);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE,
                            "Wrong transaction detected, find it and remove it from the csv " + transaction, e);
                }
            }
            if (totalQuantity > 0) {
                hasTransactions = true;
                minProfitable = accumulated / totalQuantity;
            } else {
                minProfitable = 0.0;
            }
        }
        return new TransactionsSummary(hasTransactions, minProfitable, lowestPurchase, lastPurchase, buys, sells);
    }

    public static boolean isPanicSellInDays(List<CsvTransactionRow> previousTransactions, Date deadLine) {
        if (previousTransactions == null || previousTransactions.isEmpty()) {
            return false;
        } else {
            for (int i = previousTransactions.size() - 1; i >= 0; i--) {
                CsvTransactionRow tx = previousTransactions.get(i);
                if (deadLine.getTime() > tx.getDate().getTime()) {
                    return false;
                } else if (tx.getSide() == Action.SELL_PANIC) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Map<String, String>> userUsdt(Date now, Map<String, Double> prices, Map<String, String> wallet) {
        Map<String, String> walletInUsdt = walletInSymbolUsdt(prices, wallet);
        String dateStr = Utils.fromDate(FORMAT_SECOND, now);
        List<Map<String, String>> rows = new ArrayList<>();
        for (Entry<String, String> entry : wallet.entrySet()) {
            String symbol = Utils.USDT.equals(entry.getKey()) ? Utils.USDT : entry.getKey() + Utils.USDT;
            Map<String, String> row = new LinkedHashMap<>();
            row.put("DATE", dateStr);
            row.put("SYMBOL", entry.getKey());
            row.put("SYMBOL_VALUE", entry.getValue());
            String usdt = walletInUsdt.get(symbol);
            if (usdt != null) {
                row.put(Utils.USDT, usdt);
                double val = Double.parseDouble(usdt);
                // Don't save very small values
                if (val > MIN_WALLET_VALUE_TO_RECORD) {
                    rows.add(row);
                }
            }
        }
        Map<String, String> row = new LinkedHashMap<>();
        row.put("DATE", dateStr);
        row.put("SYMBOL", TOTAL_USDT);
        row.put("SYMBOL_VALUE", walletInUsdt.get(TOTAL_USDT));
        row.put(Utils.USDT, walletInUsdt.get(TOTAL_USDT));
        rows.add(row);
        return rows;
    }

    public static Map<String, String> walletInSymbolUsdt(Map<String, Double> prices, Map<String, String> wallet) {
        Map<String, String> walletUsdt = new LinkedHashMap<>();
        double totalUsdt = 0;
        for (Entry<String, String> entry : wallet.entrySet()) {
            double value = Double.parseDouble(entry.getValue());
            if (Utils.USDT.equals(entry.getKey())) {
                totalUsdt = totalUsdt + value;
                walletUsdt.put(Utils.USDT, entry.getValue());
            } else {
                String symbol = entry.getKey() + Utils.USDT;
                Double price = prices.get(symbol);
                if (price != null) {
                    // Some wallets have some coins that does not exist in the prices. For example
                    // in Binance: NFTUSDT or LDBNBUSDT
                    double usdt = (value * price);
                    totalUsdt = totalUsdt + usdt;
                    walletUsdt.put(symbol, Utils.format(usdt));
                }
            }
        }
        walletUsdt.put(TOTAL_USDT, Utils.format(totalUsdt));
        return walletUsdt;
    }

    /*
     * To smooth functions Constant is between 0 and 1 and defines how smooth is it
     * Y is the new value prevousResult is the previous result
     */
    public static double ewma(double constant, double y, Double previousResult) {
        if (previousResult == null) {
            return y;
        } else {
            return (constant * y) + (1 - constant) * previousResult;
        }
    }

    public static double dynamicEwma(double constantYHigher, double constantYLower, double y, Double previousResult) {
        if (previousResult == null) {
            return y;
        } else {
            if (y < previousResult) {
                return ewma(constantYLower, y, previousResult);
            } else {
                return ewma(constantYHigher, y, previousResult);
            }
        }
    }

    public static String filterLotSizeQuantity(String quantity, String minQty, String maxQty, String stepSize) {
        BigDecimal quantityD = new BigDecimal(quantity);
        BigDecimal minQtyD = new BigDecimal(minQty);
        BigDecimal maxQtyD = new BigDecimal(maxQty);
        if (quantityD.compareTo(minQtyD) < 0) {
            LOGGER.warning(quantity + " is lower than minQty " + minQty + ". The quantity is modified");
            quantityD = minQtyD;
        } else if (quantityD.compareTo(maxQtyD) > 0) {
            LOGGER.warning(quantity + " is higher than maxQty " + maxQty + ". The quantity is modified");
            quantityD = maxQtyD;
        } else {
            BigDecimal bd = new BigDecimal(quantity);
            BigDecimal mod = bd.remainder(new BigDecimal(stepSize));
            BigDecimal result = bd.subtract(mod);
            quantityD = result;
        }
        return format(quantityD);
    }

    public static void sortForChart(List<String> symbols) {
        Collections.sort(symbols, (c1, c2) -> {
            if (c1.startsWith("BUY") || c1.startsWith("SELL")) {
                return -1;
            } else if (c2.startsWith("BUY") || c2.startsWith("SELL")) {
                return 1;
            }
            return c1.compareTo(c2);
        });
    }

    public static Date dateRoundedTo10Min(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND, 0);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.set(Calendar.MINUTE, minute - (minute % 10));
        return calendar.getTime();
    }

    public static double calculateFactor(CsvRow min, CsvRow max) {
        double factor = 1 - (min.getPrice() / max.getPrice());
//      LOGGER.info(() -> "MIN is " + min.getPrice() + " MAX is " + max.getPrice() + ". Factor " + factor);
        return factor;
    }

    public static CsvRow getMinMax(List<CsvRow> values, boolean min) {
        CsvRow result = values.get(0);
        for (CsvRow row : values) {
            if ((min && row.getPrice() < result.getPrice()) || (!min && row.getPrice() > result.getPrice())) {
                result = row;
            }
        }
        return result;
    }

    /**
     * Return true if current value is higher than percentile, otherwise false;
     * 
     * @param percentile
     * @param currentValue
     * @param min
     * @param max
     * @return
     */
    public static boolean inPercentile(double percentile, double currentValue, double min, double max) {
        double normalizedCurrentValue = currentValue - min;
        double normalizedMax = max - min;
        return (normalizedCurrentValue / normalizedMax) > percentile;
    }

    public static boolean isMax(List<? extends IRow> rows) {
        if (rows.size() > 2) {
            double newest = rows.get(rows.size() - 1).getPrice();
            double middle = rows.get(rows.size() - 2).getPrice();
            double oldest = rows.get(rows.size() - 3).getPrice();
            return middle >= newest && middle > oldest;
        }
        return false;
    }

    public static boolean isMin(List<? extends IRow> rows) {
        if (rows.size() > 2) {
            double newest = rows.get(rows.size() - 1).getPrice();
            double middle = rows.get(rows.size() - 2).getPrice();
            double oldest = rows.get(rows.size() - 3).getPrice();
            return middle <= newest && middle < oldest && newest <= oldest;
        }
        return false;
    }

    public static double minProfitSellAfterDays(Date lastPurchase, Date now, double minProfitBenefit,
            double substractor, double limit, double lowestLimit) {
        int daysInBetween = (int) ((now.getTime() - lastPurchase.getTime()) / MILLIS_IN_DAY);
        double result = minProfitBenefit + (substractor * daysInBetween);
        if (result <= lowestLimit) {
            return lowestLimit;
        } else if (result >= limit) {
            return limit;
        } else {
            return result;
        }
    }

    public static double symbolValue(double currentUsdt, double usdOfUnit) {
        return currentUsdt / usdOfUnit;
    }

    public static double usdValue(double currentSymbol, double usdOfUnit) {
        return currentSymbol * usdOfUnit;
    }

    public static double usdUnitValue(double size, double usd) {
        return usd / size;
    }

    public static double applyCommission(double originalPrice, double commission) {
        return originalPrice * (1 - commission);
    }

    public static double factorMultiplier(double factor, double multiplier) {
        double result = factor * multiplier;
        if (result > 1) {
            return 1;
        } else {
            return result;
        }
    }

    /**
     * Higher priority to symbols with NO previous purchases. Then the factor.
     */
    public static List<Broker> sortBrokers(Map<String, Broker> minMax) {
        return minMax.values().stream().sorted((e2, e1) -> {
            if (!e2.getPreviousTransactions().isHasTransactions() && e1.getPreviousTransactions().isHasTransactions()) {
                return -1;
            } else if (e2.getPreviousTransactions().isHasTransactions()
                    && !e1.getPreviousTransactions().isHasTransactions()) {
                return 1;
            } else {
                return Double.compare(e1.getFactor(), e2.getFactor());
            }
        }).collect(Collectors.toList());
    }

    public static boolean isFearMode(int fearGreedIndex) {
        return fearGreedIndex < 30;
    }

    public static double factorFearGreedAdjusted(double factorBase, CsvRow row, double benefitsAvg) {
        double adjustedFactor = factorBase;
        int fearGreedIndex = row.getFearGreedIndex();
        if (!bigVariationFearGreedIndex(row.getFearGreedIndexAvg(), fearGreedIndex)) {
            // It has been long time in fear, lets make it normal
            adjustedFactor = factorBase;
        } else {
            if (fearGreedIndex < 10) {
                adjustedFactor = factorBase + 0.2;
            } else if (fearGreedIndex < 15) {
                adjustedFactor = factorBase + 0.15;
            } else if (fearGreedIndex < 20) {
                adjustedFactor = factorBase + 0.1;
            } else if (fearGreedIndex < 30) {
                adjustedFactor = factorBase + 0.05;
            }
        }
//    	if (benefitsAvg < 0) {
//			adjustedFactor = adjustedFactor + (benefitsAvg * -1 * 0.1);
//		}
        return adjustedFactor;
    }

    public static boolean bigVariationFearGreedIndex(double avgIndex, int currentIndex) {
        if (currentIndex >= avgIndex) {
            return false;
        } else {
            double result = (1 - (currentIndex / avgIndex));
            return result > 0.2;
        }
    }

    public static boolean isTime(Date now, int expectedHour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour == expectedHour) {
            int minute = calendar.get(Calendar.MINUTE);
            if (minute >= 0 && minute <= 10) {
                return true;
            }
        }
        return false;
    }

    public static double benefit(double minProfitableSellPrice, double currentPrice) {
        if (currentPrice > minProfitableSellPrice) {
            return 1 - (minProfitableSellPrice / currentPrice);
        } else {
            return -1 * (1 - (currentPrice / minProfitableSellPrice));
        }
    }

    public static Date getStartOfSpecifiedMonth(Date now, int nextMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getDateOfDaysBackZero(now, 0));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, nextMonths);
        return calendar.getTime();
    }

    public static boolean isLowerPurchase(double currentPrice, double lowestPurchase, double minimizer) {
        // Purchase is lower than X% of last purchase
        return currentPrice < (lowestPurchase * minimizer);
    }

    public static CsvTransactionRow calculatedUsdtCsvTransactionRow(Date date, String symbol, String orderId,
            Action action, String usdt, double currentUsdtPrice, double commission) {
        double expectedQuantity = Utils.symbolValue(Double.parseDouble(usdt), currentUsdtPrice);
        expectedQuantity = Utils.applyCommission(expectedQuantity, commission);
        CsvTransactionRow tx = new CsvTransactionRow(date, orderId, action, symbol, usdt,
                Utils.format(expectedQuantity), currentUsdtPrice);
        return tx;

    }

    public static CsvTransactionRow calculatedSymbolCsvTransactionRow(Date date, String symbol, String orderId,
            Action action, String quantity, double currentUsdtPrice, double commission) {
        double expectedUsdt = Utils.usdValue(Double.parseDouble(quantity), currentUsdtPrice);
        expectedUsdt = Utils.applyCommission(expectedUsdt, commission);
        CsvTransactionRow tx = new CsvTransactionRow(date, orderId, action, symbol, Utils.format(expectedUsdt),
                quantity, currentUsdtPrice);
        return tx;

    }

    public static List<CsvTransactionRow> openPossitions(List<Broker> brokers,
            List<CsvTransactionRow> newTransactions) {
        Map<String, List<CsvTransactionRow>> previousTx = brokers.stream()
                .flatMap(broker -> broker.getPreviousTransactions().getPreviousBuys().stream())
                .collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
        return openPossitions2(previousTx, newTransactions);
    }

    public static List<CsvTransactionRow> openPossitions2(Map<String, List<CsvTransactionRow>> previousTx,
            List<CsvTransactionRow> newTransactions) {
        for (CsvTransactionRow newTransaction : newTransactions) {
            if (newTransaction.getSide() == Action.BUY) {
                List<CsvTransactionRow> bySymbol = previousTx.get(newTransaction.getSymbol());
                if (bySymbol == null) {
                    bySymbol = new ArrayList<>();
                    previousTx.put(newTransaction.getSymbol(), bySymbol);
                }
                bySymbol.add(newTransaction);
            } else if (newTransaction.getSide() == Action.SELL || newTransaction.getSide() == Action.SELL_PANIC) {
                previousTx.remove(newTransaction.getSymbol());
            } else {
                throw new IllegalArgumentException(newTransaction + " side is not understood!");
            }
        }
        return previousTx.values().stream().flatMap(val -> val.stream())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate())).collect(Collectors.toList());
    }

    public static List<CsvProfitRow> profitsDaysBack(Date now, int days, List<CsvProfitRow> profits) {
        Date from = getDateOfDaysBack(now, days);
        List<CsvProfitRow> filtered = profits.stream().filter(row -> row.getSellDate().getTime() >= from.getTime())
                .collect(Collectors.toList());
        return filtered;
    }
    
    public static String profitSummary(Date now, int days, List<CsvProfitRow> profits) {
        double totalUsdtBuy = 0;
        double totalUsdtSell = 0;
        List<CsvProfitRow> filtered = profitsDaysBack(now, days, profits);
        for (CsvProfitRow row : filtered) {
            totalUsdtBuy = totalUsdtBuy + Double.parseDouble(row.getQuantityUsdtBuy());
            totalUsdtSell = totalUsdtSell + Double.parseDouble(row.getQuantityUsdtSell());
        }
        double result = totalUsdtSell - totalUsdtBuy;
        double resultPercent = 0;
        if (result != 0) {
            resultPercent = result * 100 / totalUsdtBuy;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Last ").append(days).append(" days: ").append(Utils.format(totalUsdtBuy, 2)).append("$-")
                .append(Utils.format(totalUsdtSell, 2)).append("$, ").append(Utils.format(result, 2)).append("$(")
                .append(Utils.format(resultPercent, 2)).append("%)");
        if (resultPercent < 0) {
            builder.append(" ❌");
        } else {
            builder.append(" ✅");
        }
        return builder.toString();
    }

    public static Map<String, Double> calculateBenefits(List<Broker> brokers) {
        Map<String, Double> benefits = new HashMap<>();
        double sumBenefits = 0;
        int count = 0;
        for (Broker broker : brokers) {
            TransactionsSummary summary = broker.getPreviousTransactions();
            if (summary.isHasTransactions()) {
                count++;
                double benefit = Utils.benefit(summary.getMinProfitable(), broker.getNewest().getPrice());
                sumBenefits = benefit + sumBenefits;
                benefits.put(broker.getSymbol(), benefit);
            }
        }
        if (count > 0) {
            benefits.put(BENEFITS_AVG, sumBenefits / count);
        } else {
            benefits.put(BENEFITS_AVG, 0.0);
        }
        return benefits;
    }

    public static boolean resyncTransactions(SecuredAPI securedApi, List<CsvTransactionRow> transactions) {
        // Limit to avoid many requests
        int LIMIT_RESYNC = 5;
        int synced = 0;
        for (int i = 0; i < transactions.size(); i++) {
            if (synced < LIMIT_RESYNC) {
                CsvTransactionRow tx = transactions.get(i);
                if (!tx.isSync()) {
                    CsvTransactionRow syncedTx = securedApi.synchronize(tx);
                    transactions.set(i, syncedTx);
                    synced++;
                }
            } else {
                break;
            }
        }
        return synced > 0;
    }

    public static boolean resyncProfit(Map<String, CsvTransactionRow> byOrderId, List<CsvProfitRow> profitRows,
            String commission) {
        double commissionD = Double.parseDouble(commission);
        int synced = 0;
        for (int i = 0; i < profitRows.size(); i++) {
            CsvProfitRow profitRow = profitRows.get(i);
            if (!profitRow.isSync()) {
                String[] buyIds = profitRow.getBuyIds().split(ORDER_ID_SPLIT);
                String sellId = profitRow.getSellId();
                CsvTransactionRow sellTx = byOrderId.get(sellId);
                if (sellTx != null && sellTx.isSync()) {
                    boolean process = true;
                    double totalQuantityBuy = 0;
                    double totalUsdBuy = 0;
                    for (String buyId : buyIds) {
                        CsvTransactionRow tx = byOrderId.get(buyId);
                        if (tx != null && tx.isSync()) {
                            totalUsdBuy = totalUsdBuy + Double.parseDouble(tx.getUsdt());
                            totalQuantityBuy = totalQuantityBuy + Double.parseDouble(tx.getQuantity());
                        } else {
                            LOGGER.warning("No transaction with ID: " + buyId
                                    + " was found. Possibly transaction is too old or ID is wrong. Skipping CsvProfitRow.");
                            process = false;
                            break;
                        }
                    }
                    if (process) {
                        double usdtProfit = Double.parseDouble(sellTx.getUsdt()) - totalUsdBuy;
                        double commissionUsdt = usdtProfit * commissionD;
                        double netUsdtProfit = usdtProfit - commissionUsdt;
                        double profitPercentage = usdtProfit * 100 / totalUsdBuy;
                        CsvProfitRow newCsvProfitRow = new CsvProfitRow(profitRow, Utils.format(totalQuantityBuy, 2),
                                Utils.format(totalUsdBuy, 2), Utils.format(Double.parseDouble(sellTx.getQuantity()), 2),
                                Utils.format(Double.parseDouble(sellTx.getUsdt()), 2), Utils.format(commissionUsdt, 2),
                                Utils.format(usdtProfit, 2), Utils.format(netUsdtProfit, 2),
                                Utils.format(profitPercentage, 2) + "%", Utils.format(commissionD * 100, 2) + "%");
                        profitRows.set(i, newCsvProfitRow);
                        synced++;
                    }
                }
            }
        }
        return synced > 0;
    }

    public static Map<String, Double> simplePrices(Map<String, Price> prices) {
        return prices.values().stream().collect(Collectors.toMap(Price::getSymbol, Price::getPrice));
    }

    // Get the USDT of the wallet when there is nothing else
    public static String baseUsdt(Collection<String> excludeLimits, List<Map<String, String>> original) {
        List<Map<String, String>> wallet = new ArrayList<>(original);
        wallet.removeIf(
                entry -> excludeLimits.contains(entry.get("SYMBOL") + USDT) || TOTAL_USDT.equals(entry.get("SYMBOL")));
        if (wallet.size() == 1) {
            Map<String, String> usdt = wallet.get(0);
            if (Utils.USDT.equals(usdt.get("SYMBOL"))) {
                return usdt.get(Utils.USDT);
            } else {
                LOGGER.warning("Unexpected symbol found. Symbol should be USDT: " + usdt);
            }
        }
        return null;
    }

    public static long delayto30or00(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        if (cal.get(Calendar.MINUTE) < 30) {
            Calendar cal30 = Calendar.getInstance();
            cal30.setTime(now);
            cal30.set(Calendar.MINUTE, 30);
            return cal30.getTimeInMillis() - now.getTime();
        } else {
            Calendar cal00 = Calendar.getInstance();
            cal00.setTime(now);
            cal00.set(Calendar.MINUTE, 0);
            cal00.add(Calendar.HOUR_OF_DAY, 1);
            return cal00.getTimeInMillis() - now.getTime();
        }
    }

    public static double totalQuantity(double limitUsdAmount, List<ClosePositionResponse> positions) {
        double quantity = 0;
        for (ClosePositionResponse position : positions) {
            double spent = limitUsdAmount * position.size;
            double currentPrice = Double.parseDouble(position.open_price);
            quantity = quantity + symbolValue(spent, currentPrice);
        }
        return quantity;
    }

    public static List<String> splitSize(String bigString, int size) {
        List<String> ret = new ArrayList<>((bigString.length() + size - 1) / size);
        for (int start = 0; start < bigString.length(); start += size) {
            ret.add(bigString.substring(start, Math.min(bigString.length(), start + size)));
        }
        return ret;
    }
}
