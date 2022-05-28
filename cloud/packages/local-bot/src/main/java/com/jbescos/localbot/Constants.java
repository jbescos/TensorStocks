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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbescos.exchange.PropertiesBinance;
import com.jbescos.exchange.PropertiesKucoin;


public class Constants implements PropertiesBinance, PropertiesKucoin {

	private static final String PROPERTY_PATH = System.getProperty("property");
	public static final ObjectMapper MAPPER = new ObjectMapper();
	// 5 Minutes
	public static final long SYMBOL_VERIFICATION_TIME = 1000 * 60 * 5;
	public static final String USDT = "USDT";
	public static final double PROFIT;
	public static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws";
	public static final List<String> SYMBOLS;
	public static final long LATENCY;
	public static final String BINANCE_PUBLIC_KEY;
	public static final String BINANCE_PRIVATE_KEY;
	public static final String KUCOIN_PUBLIC_KEY;
	public static final String KUCOIN_PRIVATE_KEY;
	public static final String KUCOIN_API_PASS_PHRASE;
	public static final String KUCOIN_API_VERSION;
    public static final double KUCOIN_BUY_COMMISSION;
    public static final double KUCOIN_SELL_COMMISSION;
	public static final DateFormat FORMAT_SECOND = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final int WORKERS;
	public static final BigDecimal COMMISSION_APPLIED;
	public static final BigDecimal MIN_BINANCE_USDT;
	private static final DecimalFormat DECIMAL_FORMAT;
	
	static {
		try {
			Properties properties = load(PROPERTY_PATH);
			PROFIT = Double.parseDouble(properties.getProperty("profit"));
			SYMBOLS = Arrays.asList(properties.getProperty("bot.symbols").split(","));
			LATENCY = Long.parseLong(properties.getProperty("message.millis.latency"));
			BINANCE_PUBLIC_KEY = properties.getProperty("binance.public.key");
			BINANCE_PRIVATE_KEY = properties.getProperty("binance.private.key");
			KUCOIN_PUBLIC_KEY = properties.getProperty("kucoin.public.key");
			KUCOIN_PRIVATE_KEY = properties.getProperty("kucoin.private.key");
			KUCOIN_API_PASS_PHRASE = properties.getProperty("kucoin.passphrase.key");
			KUCOIN_API_VERSION = properties.getProperty("kucoin.version");
			KUCOIN_BUY_COMMISSION = Double.parseDouble(properties.getProperty("kucoin.buy.commission"));
			KUCOIN_SELL_COMMISSION = Double.parseDouble(properties.getProperty("kucoin.sell.commission"));
			WORKERS = Integer.parseInt(properties.getProperty("bot.workers"));
			COMMISSION_APPLIED = new BigDecimal(1).add(new BigDecimal(properties.getProperty("binance.commission")));
			MIN_BINANCE_USDT = new BigDecimal(properties.getProperty("binance.minimum.usdt"));
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
	
	public static String format(Date date) {
		return FORMAT_SECOND.format(date);
	}

	@Override
	public String binancePublicKey() {
		return BINANCE_PUBLIC_KEY;
	}

	@Override
	public String binancePrivateKey() {
		return BINANCE_PRIVATE_KEY;
	}

	@Override
	public String kucoinPublicKey() {
		return KUCOIN_PUBLIC_KEY;
	}

	@Override
	public String kucoinPrivateKey() {
		return KUCOIN_PRIVATE_KEY;
	}

	@Override
	public String kucoinApiPassPhrase() {
		return KUCOIN_API_PASS_PHRASE;
	}

	@Override
	public String kucoinApiVersion() {
		return KUCOIN_API_VERSION;
	}

	@Override
	public double kucoinBuyCommission() {
		return KUCOIN_BUY_COMMISSION;
	}

	@Override
	public double kucoinSellCommission() {
		return KUCOIN_SELL_COMMISSION;
	}
}
