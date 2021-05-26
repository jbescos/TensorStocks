package com.jbescos.localbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;


public class Constants {

	private static final String PROPERTY_PATH = System.getProperty("property");
	public static final String USDT = "USDT";
	public static final String WS_URL;
	public static final List<String> SYMBOLS;
	public static final long LATENCY;
	public static final String BINANCE_PUBLIC_KEY;
	public static final String BINANCE_PRIVATE_KEY;
	public static final DateFormat FORMAT_SECOND = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final int WORKERS;
	public static final BigDecimal COMMISSION_APPLIED;
	public static final BigDecimal MIN_BINANCE_USDT;
	public static final BigDecimal AMOUNT_REDUCER;
	
	static {
		try {
			Properties properties = load(PROPERTY_PATH);
			WS_URL = properties.getProperty("binance.ws.url");
			SYMBOLS = Arrays.asList(properties.getProperty("bot.symbols").toLowerCase().split(","));
			LATENCY = Long.parseLong(properties.getProperty("message.millis.latency"));
			BINANCE_PUBLIC_KEY = properties.getProperty("binance.public.key");
			BINANCE_PRIVATE_KEY = properties.getProperty("binance.private.key");
			WORKERS = Integer.parseInt(properties.getProperty("bot.workers"));
			COMMISSION_APPLIED = new BigDecimal(1).subtract(new BigDecimal(properties.getProperty("binance.commission")));
			MIN_BINANCE_USDT = new BigDecimal(properties.getProperty("binance.minimum.usdt"));
			AMOUNT_REDUCER = new BigDecimal(properties.getProperty("amount.reducer"));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load properties " + PROPERTY_PATH, e);
		}
	}
	
	private static Properties load(String properties) throws IOException {
		if (properties == null) {
			try (InputStream in = Constants.class.getResourceAsStream("/config.properties")) {
				return load(in);
			}
		} else {
			try (FileInputStream fileInput = new FileInputStream(new File(PROPERTY_PATH))) {
				return load(fileInput);
			}
		}
	}
	
	private static Properties load(InputStream in) throws IOException {
		if (in != null) {
			Properties prop = new Properties();
			prop.load(in);
			return prop;
		}
		return null;
	}
	
	public static String format(double amount) {
		return String.format(Locale.US, "%.8f", amount);
	}
	
}
