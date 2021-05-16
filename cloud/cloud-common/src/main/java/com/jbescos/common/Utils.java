package com.jbescos.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jbescos.common.Account.Balances;

public class Utils {
	
	public static final double BUY_COMISSION = 0.02;
	public static final double MIN_MAX_FACTOR = 0.2;
	public static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat FORMAT_SECOND = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String USDT = "USDT";

	public static Properties fromClasspath(String properties) throws IOException {
		Properties prop = new Properties();
		try (InputStream in = DataBase.class.getResourceAsStream(properties)) {
			prop.load(in);
		}
		return prop;
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
}
