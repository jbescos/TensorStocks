package com.jbescos.localbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
	public static final BigDecimal EWMA_CONSTANT;
	private static final DecimalFormat DECIMAL_FORMAT;
	
	static {
		try {
			Properties properties = load(PROPERTY_PATH);
			WS_URL = properties.getProperty("binance.ws.url");
			SYMBOLS = Arrays.asList(properties.getProperty("bot.symbols").toLowerCase().split(","));
			LATENCY = Long.parseLong(properties.getProperty("message.millis.latency"));
			BINANCE_PUBLIC_KEY = properties.getProperty("binance.public.key");
			BINANCE_PRIVATE_KEY = properties.getProperty("binance.private.key");
			WORKERS = Integer.parseInt(properties.getProperty("bot.workers"));
			COMMISSION_APPLIED = new BigDecimal(1).add(new BigDecimal(properties.getProperty("binance.commission")));
			MIN_BINANCE_USDT = new BigDecimal(properties.getProperty("binance.minimum.usdt"));
			AMOUNT_REDUCER = new BigDecimal(properties.getProperty("amount.reducer"));
			EWMA_CONSTANT = new BigDecimal(properties.getProperty("ewma.constant"));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load properties " + PROPERTY_PATH, e);
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
		symbols.setDecimalSeparator('.');
		DECIMAL_FORMAT = new DecimalFormat("0", symbols);
		DECIMAL_FORMAT.setRoundingMode(RoundingMode.DOWN);
		DECIMAL_FORMAT.setMaximumFractionDigits(8);
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
	
	public static String format(BigDecimal amount) {
		return DECIMAL_FORMAT.format(amount);
	}
	
	public static BigDecimal ewma(BigDecimal y, BigDecimal prevousResult) {
		if (prevousResult == null) {
			return y;
		} else {
			BigDecimal firstPart = EWMA_CONSTANT.multiply(y);
			BigDecimal secondPart = prevousResult.multiply(new BigDecimal(1).subtract(EWMA_CONSTANT));
			return firstPart.add(secondPart).setScale(8, RoundingMode.CEILING);
		}
	}
	
	public static String format(Date date) {
		return FORMAT_SECOND.format(date);
	}
}
