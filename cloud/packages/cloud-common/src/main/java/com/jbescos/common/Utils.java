package com.jbescos.common;

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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.jbescos.common.Account.Balances;
import com.jbescos.common.BuySellAnalisys.Action;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    public static final long MINUTES_30_MILLIS = 30 * 60 * 1000;
	public static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat FORMAT_SECOND = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String USDT = "USDT";
	public static final String NEW_LINE = "\r\n";
	public static final String CSV_ROW_HEADER = "DATE,SYMBOL,PRICE,AVG,AVG_2,VOLUME_BUY,VOLUME_TOTAL,OPEN_PRICE,CLOSE_PRICE" + NEW_LINE;
	public static final String LAST_PRICE = "data/last_price.csv";
	public static final String EMPTY_STR = "";
	

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
	
	public static List<String> daysBack(Date currentTime, int daysBack, String prefix, String subfix) {
		List<String> days = new ArrayList<>(daysBack);
		Calendar c = Calendar.getInstance();
		c.setTime(currentTime);
		c.add(Calendar.DAY_OF_YEAR, daysBack * -1);
		for (int i = 0; i < daysBack; i++) {
			c.add(Calendar.DAY_OF_YEAR, 1);
			days.add(prefix + fromDate(FORMAT, c.getTime()) + subfix);
		}
		return days;
	}
	
	public static Date getDateOfDaysBack(Date currentTime, int daysBack) {
		Calendar c = Calendar.getInstance();
		c.setTime(currentTime);
		c.add(Calendar.DAY_OF_YEAR, daysBack * -1);
		return c.getTime();
	}
	
	public static Date fromString(DateFormat format, String date) {
		try {
			return format.parse(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Cannot parse " + date, e);
		}
	}
	
	public static String fromDate(DateFormat format, Date date) {
		return format.format(date);
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
	
	public static String format(double amount) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
		symbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0", symbols);
		df.setRoundingMode(RoundingMode.DOWN);
        df.setMaximumFractionDigits(8);
		return df.format(amount);
	}
	
	public static String format(BigDecimal amount) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
		symbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0", symbols);
		df.setRoundingMode(RoundingMode.DOWN);
        df.setMaximumFractionDigits(8);
		return df.format(amount);
	}
	
	public static double minSellProfitable(List<CsvTransactionRow> previousTransactions) {
		if (previousTransactions == null || previousTransactions.isEmpty()) {
			return 0.0;
		}
		double accumulated = 0.0;
		double totalQuantity = 0.0;
		String symbol = null;
		for (CsvTransactionRow transaction : previousTransactions) {
			if (symbol == null) {
				symbol = transaction.getSymbol();
			} else if (!symbol.equals(transaction.getSymbol())) {
				throw new IllegalArgumentException("Every CsvAccountRow must contain the same symbol. It was found " + symbol + " and " + transaction.getSymbol());
			}
			if (transaction.getSide() == Action.BUY) {
				totalQuantity = totalQuantity + transaction.getQuantity();
				accumulated = accumulated + transaction.getUsdt();
			} else {
				accumulated = accumulated - transaction.getUsdt();
				totalQuantity = totalQuantity - transaction.getQuantity();
			}
		}
		if (totalQuantity > 0) {
			return accumulated / totalQuantity;
		} else {
			return 0.0;
		}
	}
	
	public static List<Map<String, String>> userUsdt(Date now, List<Price> prices, Account account) {
		List<Map<String, String>> rows = new ArrayList<>();
		double totalUsdt = 0.0;
		String dateStr = Utils.fromDate(Utils.FORMAT_SECOND, now);
		for (Balances balance : account.getBalances()) {
			double value = Double.parseDouble(balance.getFree()) + Double.parseDouble(balance.getLocked());
			Map<String, String> row = new LinkedHashMap<>();
			row.put("DATE", dateStr);
			row.put("SYMBOL", balance.getAsset());
			row.put("SYMBOL_VALUE", Double.toString(value));
			String symbol = balance.getAsset() + "USDT";
			boolean isUsdtConvertible = false;
			if (Utils.USDT.equals(balance.getAsset())) {
				row.put(Utils.USDT, Double.toString(value));
				totalUsdt = totalUsdt + value;
				isUsdtConvertible = true;
			} else {
				for (Price price : prices) {
					if(symbol.equals(price.getSymbol())) {
						double usdt = (value * price.getPrice());
						row.put(Utils.USDT, Double.toString(usdt));
						totalUsdt = totalUsdt + usdt;
						isUsdtConvertible = true;
						break;
					}
				}
			}
			if (isUsdtConvertible) {
				rows.add(row);
			}
		}
		Map<String, String> row = new LinkedHashMap<>();
		row.put("DATE", dateStr);
		row.put("SYMBOL", "TOTAL_USDT");
		row.put("SYMBOL_VALUE", Double.toString(totalUsdt));
		row.put(Utils.USDT, Double.toString(totalUsdt));
		rows.add(row);
		return rows;
	}
	
	/*
	 *  To smooth functions
	 *  Constant is between 0 and 1 and defines how smooth is it
	 *  Y is the new value
	 *  prevousResult is the previous result
	 */
	public static double ewma(double contant, double y, Double previousResult) {
		if (previousResult == null) {
			return y;
		} else {
			return (contant * y) + (1 - contant) * previousResult;
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
}
