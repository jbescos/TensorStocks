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
import com.jbescos.common.Broker.Action;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    public static final long MINUTES_30_MILLIS = 30 * 60 * 1000;
    public static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
    public static final DateFormat FORMAT_MONTH = new SimpleDateFormat("yyyy-MM");
	public static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat FORMAT_SECOND = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String USDT = "USDT";
	public static final String NEW_LINE = "\r\n";
	public static final String CSV_ROW_HEADER = "DATE,SYMBOL,PRICE,AVG,AVG_2,VOLUME_BUY,VOLUME_TOTAL,OPEN_PRICE,CLOSE_PRICE" + NEW_LINE;
	public static final String TX_ROW_HEADER = "DATE,ORDER_ID,SIDE,SYMBOL,USDT,QUANTITY,USDT_UNIT" + NEW_LINE;
	public static final String LAST_PRICE = "data/last_price.csv";
	public static final String EMPTY_STR = "";
	public static final String TRANSACTIONS_PREFIX = "transactions/transactions_";
	public static final String WALLET_PREFIX = "wallet/wallet_";
	public static final double MIN_WALLET_VALUE_TO_RECORD = 0.1;
	

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

	private static List<String> dateBack(Date currentTime, int unitBack, String prefix, String subfix, int dateType, DateFormat dateFormat) {
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

	public static Date getHoursOfDaysBack(Date currentTime, int hoursBack) {
		Calendar c = Calendar.getInstance();
		c.setTime(currentTime);
		c.add(Calendar.HOUR, hoursBack * -1);
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
	
    public static String thisMonth() {
        return fromDate(FORMAT_MONTH, new Date());
    }
   
    public static String thisMonth(Date date) {
        return fromDate(FORMAT_MONTH, date);
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
	
	public static TransactionsSummary minSellProfitable(List<CsvTransactionRow> previousTransactions) {
		Date lastPurchase = null;
		List<CsvTransactionRow> buys = new ArrayList<>();
		List<CsvTransactionRow> sells = new ArrayList<>();
		boolean hasTransactions = false;
		double minProfitable = 0;
		if (previousTransactions == null || previousTransactions.isEmpty()) {
			minProfitable = 0;
		} else {
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
					if (lastPurchase == null) {
						lastPurchase = transaction.getDate();
					}
					totalQuantity = totalQuantity + Double.parseDouble(transaction.getQuantity());
					accumulated = accumulated + Double.parseDouble(transaction.getUsdt());
					buys.add(transaction);
				} else {
					accumulated = accumulated - Double.parseDouble(transaction.getUsdt());
					totalQuantity = totalQuantity - Double.parseDouble(transaction.getQuantity());
					sells.add(transaction);
				}
			}
			if (totalQuantity > 0) {
				hasTransactions = true;
				minProfitable = accumulated / totalQuantity;
			} else {
				minProfitable = 0.0;
			}
		}
		return new TransactionsSummary(hasTransactions, minProfitable, lastPurchase, buys, sells);
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
	
	public static List<Map<String, String>> userUsdt(Date now, List<Price> prices, Account account) {
		List<Map<String, String>> rows = new ArrayList<>();
		double totalUsdt = 0.0;
		String dateStr = Utils.fromDate(Utils.FORMAT_SECOND, now);
		for (Balances balance : account.getBalances()) {
			double value = Double.parseDouble(balance.getFree()) + Double.parseDouble(balance.getLocked());
			Map<String, String> row = new LinkedHashMap<>();
			row.put("DATE", dateStr);
			row.put("SYMBOL", balance.getAsset());
			row.put("SYMBOL_VALUE", Utils.format(value));
			String symbol = balance.getAsset() + "USDT";
			boolean isUsdtConvertible = false;
			if (Utils.USDT.equals(balance.getAsset())) {
				row.put(Utils.USDT, Utils.format(value));
				totalUsdt = totalUsdt + value;
				isUsdtConvertible = true;
			} else {
				for (Price price : prices) {
					if(symbol.equals(price.getSymbol())) {
						double usdt = (value * price.getPrice());
						row.put(Utils.USDT, Utils.format(usdt));
						totalUsdt = totalUsdt + usdt;
						isUsdtConvertible = true;
						break;
					}
				}
			}
			if (isUsdtConvertible) {
			    double val = Double.parseDouble(row.get(Utils.USDT));
			    // Don't save very small values
			    if (val > MIN_WALLET_VALUE_TO_RECORD) {
			        rows.add(row);
			    }
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
	
	public static double calculateFactor(CsvRow min, CsvRow max) {
		double factor =  1 - (min.getPrice() / max.getPrice());
//		LOGGER.info(() -> "MIN is " + min.getPrice() + " MAX is " + max.getPrice() + ". Factor " + factor);
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
	
	public static double minProfitSellAfterDays(Date lastPurchase, Date now, double minProfitBenefit, double substractor, double limit) {
	    int daysInBetween = (int)( (now.getTime() - lastPurchase.getTime()) / MILLIS_IN_DAY);
	    double result = minProfitBenefit + (substractor * daysInBetween);
	    if (result <= 0) {
	        return 0;
	    } else if (result >= limit){
	        return limit;
	    }else {
	        return result;
	    }
	}
	
	public static double symbolValue(double currentUsdt, double usdOfUnit) {
		return currentUsdt / usdOfUnit;
	}

	public static double usdValue(double currentSymbol, double usdOfUnit) {
		return currentSymbol * usdOfUnit;
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

}
