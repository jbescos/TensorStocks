package com.jbescos.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.StorageOptions;

public class CloudProperties {

	private static final Logger LOGGER = Logger.getLogger(CloudProperties.class.getName());
	private static final String PROPERTIES_BUCKET = "crypto-properties";
	private static final String PROPERTIES_FILE = "cloud.properties";
	public static final String PROJECT_ID = System.getenv("GCP_PROJECT");
	public static final String BUCKET;
	public static final String USER;
	public static final String PASSWORD;
	public static final String URL;
	public static final String DRIVER;
	public static final String BINANCE_PUBLIC_KEY;
	public static final String BINANCE_PRIVATE_KEY;
	public static final double BINANCE_MIN_TRANSACTION;
	public static final List<String> BOT_NEVER_BUY_LIST_SYMBOLS;
	public static final List<String> BOT_WHITE_LIST_SYMBOLS;
	public static final double BOT_SELL_REDUCER;
	public static final double BOT_BUY_REDUCER;
	public static final double BOT_PERCENTILE_FACTOR;
	public static final double BOT_BUY_COMISSION;
	public static final double BOT_SELL_COMISSION;
	public static final double BOT_MIN_MAX_RELATION;
	public static final String BOT_DAYS_BACK_STATISTICS;
	private static final Map<String, Double> MIN_SELL;
	public static final double EWMA_CONSTANT;

	static {
		Properties properties = null;
		try {
			properties = Utils.fromClasspath("/" + PROPERTIES_FILE);
			if (properties == null) {
				properties = new Properties();
				Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
				Page<Bucket> page = storage.list(BucketListOption.prefix(PROPERTIES_BUCKET));
				String propertiesBucket = null;
				for (Bucket bucket : page.iterateAll()) {
					propertiesBucket = bucket.getName();
					break;
				}
				if (propertiesBucket == null) {
					throw new IllegalStateException("Bucket that starts with " + PROPERTIES_BUCKET + " was not found");
				}
				try (ReadChannel readChannel = storage.reader(propertiesBucket, PROPERTIES_FILE);
						BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
					StringBuilder builder = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						builder.append(line).append("\r\n");
					}
					InputStream inputStream = new ByteArrayInputStream(
							builder.toString().getBytes(StandardCharsets.UTF_8));
					properties.load(inputStream);
				} catch (IOException e1) {
					throw new IllegalStateException("Cannot load " + PROPERTIES_FILE, e1);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load " + PROPERTIES_FILE, e);
		}
		BUCKET = properties.getProperty("storage.bucket");
		USER = properties.getProperty("database.user");
		PASSWORD = properties.getProperty("database.password");
		URL = properties.getProperty("database.url");
		DRIVER = properties.getProperty("database.driver");
		BINANCE_PUBLIC_KEY = properties.getProperty("binance.public.key");
		BINANCE_PRIVATE_KEY = properties.getProperty("binance.private.key");
		BOT_WHITE_LIST_SYMBOLS = Arrays.asList(properties.getProperty("bot.white.list").split(","));
		BOT_NEVER_BUY_LIST_SYMBOLS = Arrays.asList(properties.getProperty("bot.never.buy").split(","));
		BOT_SELL_REDUCER = Double.parseDouble(properties.getProperty("bot.sell.reducer"));
		BOT_BUY_REDUCER = Double.parseDouble(properties.getProperty("bot.buy.reducer"));
		BOT_PERCENTILE_FACTOR = Double.parseDouble(properties.getProperty("bot.percentile.factor"));
		BOT_BUY_COMISSION = Double.parseDouble(properties.getProperty("bot.buy.comission"));
		BOT_SELL_COMISSION = Double.parseDouble(properties.getProperty("bot.sell.comission"));
		BOT_MIN_MAX_RELATION = Double.parseDouble(properties.getProperty("bot.min.max.relation"));
		BOT_DAYS_BACK_STATISTICS = properties.getProperty("bot.days.back.statistics");
		BINANCE_MIN_TRANSACTION = Double.parseDouble(properties.getProperty("binance.min.transaction"));
		MIN_SELL = createMinSell(properties);
		EWMA_CONSTANT = Double.parseDouble(properties.getProperty("ewma.constant"));
	}

	private static Map<String, Double> createMinSell(Properties properties) {
		Map<String, Double> minSell = new HashMap<>();
		Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();
		while (enums.hasMoreElements()) {
			String key = enums.nextElement();
			if (key.startsWith("bot.min.sell")) {
				double value = Double.parseDouble(properties.getProperty(key));
				String symbol = key.split("\\.")[3];
				minSell.put(symbol, value);
			}
		}
		return Collections.unmodifiableMap(minSell);
	}

	public static double minSell(String symbol) {
		Double value = MIN_SELL.get(symbol);
		return value == null ? 0.0 : value;
	}
}
