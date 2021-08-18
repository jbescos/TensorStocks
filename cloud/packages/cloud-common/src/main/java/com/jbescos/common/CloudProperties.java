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
	public static final String GOOGLE_TOPIC_ID;
	public static final String GOOGLE_PROJECT_ID;
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
	public static final double BOT_PERCENTILE_BUY_FACTOR;
	public static final double BOT_PERCENTILE_SELL_FACTOR;
	public static final double BOT_BUY_COMISSION;
	public static final double BOT_SELL_COMISSION;
	public static final double BOT_MIN_MAX_RELATION_SELL_BEARISH;
	public static final double BOT_MIN_MAX_RELATION_SELL_BULLISH;
	public static final double BOT_MIN_MAX_RELATION_BUY_BEARISH;
	public static final double BOT_MIN_MAX_RELATION_BUY_BULLISH;
	public static final int BOT_DAYS_BACK_STATISTICS;
	public static final int BOT_DAYS_BACK_TRANSACTIONS;
	private static final Map<String, Double> MIN_SELL;
	public static final double EWMA_CONSTANT;
	public static final double EWMA_2_CONSTANT;
	public static final boolean BOT_SELL_IGNORE_FACTOR_REDUCER;
	public static final boolean BOT_BUY_IGNORE_FACTOR_REDUCER;
	public static final boolean PANIC_BROKER_ENABLE;
	public static final boolean GREEDY_BROKER_ENABLE;
	public static final double BOT_SELL_BENEFIT_COMPARED_TRANSACTIONS;
	public static final double BOT_PANIC_RATIO;
	public static final int BOT_PANIC_DAYS;
	public static final double BOT_GREEDY_MIN_FACTOR_BUY;
	public static final double BOT_GREEDY_MIN_PROFIT_SELL;
	public static final double BOT_GREEDY_DEFAULT_FACTOR_SELL;
	public static final int BOT_GREEDY_DAYS_TO_HOLD;
	public static final double BOT_GREEDY_IMMEDIATELY_SELL;
	public static final Map<String, FixedBuySell> FIXED_BUY_SELL;

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
		GOOGLE_TOPIC_ID = properties.getProperty("google.topic.id");
		GOOGLE_PROJECT_ID = properties.getProperty("google.project.id");
		USER = properties.getProperty("database.user");
		PASSWORD = properties.getProperty("database.password");
		URL = properties.getProperty("database.url");
		DRIVER = properties.getProperty("database.driver");
		BINANCE_PUBLIC_KEY = properties.getProperty("binance.public.key");
		BINANCE_PRIVATE_KEY = properties.getProperty("binance.private.key");
		String value = properties.getProperty("bot.white.list");
		BOT_WHITE_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() : Arrays.asList(value.split(","));
		BOT_NEVER_BUY_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() :  Arrays.asList(properties.getProperty("bot.never.buy").split(","));
		BOT_SELL_REDUCER = Double.parseDouble(properties.getProperty("bot.sell.reducer"));
		BOT_BUY_REDUCER = Double.parseDouble(properties.getProperty("bot.buy.reducer"));
		BOT_PERCENTILE_BUY_FACTOR = Double.parseDouble(properties.getProperty("bot.percentile.buy.factor"));
		BOT_PERCENTILE_SELL_FACTOR = Double.parseDouble(properties.getProperty("bot.percentile.sell.factor"));
		BOT_BUY_COMISSION = Double.parseDouble(properties.getProperty("bot.buy.comission"));
		BOT_SELL_COMISSION = Double.parseDouble(properties.getProperty("bot.sell.comission"));
		BOT_MIN_MAX_RELATION_SELL_BEARISH = Double.parseDouble(properties.getProperty("bot.min.max.relation.sell.bearish"));
		BOT_MIN_MAX_RELATION_SELL_BULLISH = Double.parseDouble(properties.getProperty("bot.min.max.relation.sell.bullish"));
		BOT_MIN_MAX_RELATION_BUY_BEARISH = Double.parseDouble(properties.getProperty("bot.min.max.relation.buy.bearish"));
		BOT_MIN_MAX_RELATION_BUY_BULLISH = Double.parseDouble(properties.getProperty("bot.min.max.relation.buy.bullish"));
		BOT_DAYS_BACK_STATISTICS = Integer.parseInt(properties.getProperty("bot.days.back.statistics"));
		BOT_DAYS_BACK_TRANSACTIONS = Integer.parseInt(properties.getProperty("bot.days.back.transactions"));
		BINANCE_MIN_TRANSACTION = Double.parseDouble(properties.getProperty("binance.min.transaction"));
		MIN_SELL = createMinSell(properties);
		EWMA_CONSTANT = Double.parseDouble(properties.getProperty("ewma.constant"));
		EWMA_2_CONSTANT = Double.parseDouble(properties.getProperty("ewma.2.constant"));
		BOT_SELL_IGNORE_FACTOR_REDUCER = Boolean.valueOf(properties.getProperty("bot.sell.ignore.factor.reducer"));
		BOT_BUY_IGNORE_FACTOR_REDUCER = Boolean.valueOf(properties.getProperty("bot.buy.ignore.factor.reducer"));
		BOT_SELL_BENEFIT_COMPARED_TRANSACTIONS = Double.parseDouble(properties.getProperty("bot.sell.benefit.compared.transactions"));
		BOT_PANIC_RATIO = Double.parseDouble(properties.getProperty("bot.panic.ratio"));
		BOT_PANIC_DAYS = Integer.parseInt(properties.getProperty("bot.panic.days"));
		BOT_GREEDY_MIN_FACTOR_BUY = Double.parseDouble(properties.getProperty("bot.greedy.min.factor.buy"));
		BOT_GREEDY_MIN_PROFIT_SELL = Double.parseDouble(properties.getProperty("bot.greedy.min.profit.sell"));
		PANIC_BROKER_ENABLE = Boolean.valueOf(properties.getProperty("bot.panic.enable"));
		GREEDY_BROKER_ENABLE = Boolean.valueOf(properties.getProperty("bot.greedy.enable"));
		BOT_GREEDY_DEFAULT_FACTOR_SELL = Double.parseDouble(properties.getProperty("bot.greedy.default.factor.to.sell"));
		BOT_GREEDY_DAYS_TO_HOLD = Integer.parseInt(properties.getProperty("bot.greedy.days.to.hold"));
		BOT_GREEDY_IMMEDIATELY_SELL = Double.parseDouble(properties.getProperty("bot.greedy.immediately.sell"));
		FIXED_BUY_SELL = fixedBuySell(properties);
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
	
	private static Map<String, FixedBuySell> fixedBuySell(Properties properties) {
		Map<String, FixedBuySell> fixedBuySell = new HashMap<>();
		Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();
		while (enums.hasMoreElements()) {
			String key = enums.nextElement();
			if (key.startsWith("bot.fixed")) {
				String symbol = key.split("\\.")[3];
				if (!fixedBuySell.containsKey(symbol)) {
					double fixedSell = Double.parseDouble(properties.getProperty("bot.fixed.sell." + symbol));
					double fixedBuy = Double.parseDouble(properties.getProperty("bot.fixed.buy." + symbol));
					FixedBuySell content = new FixedBuySell(fixedSell, fixedBuy);
					fixedBuySell.put(symbol, content);
				}
			}
		}
		return Collections.unmodifiableMap(fixedBuySell);
	}

	public static double minSell(String symbol) {
		Double value = MIN_SELL.get(symbol);
		return value == null ? 0.0 : value;
	}
	
	public static class FixedBuySell {
		private final double fixedSell;
		private final double fixedBuy;
		public FixedBuySell(double fixedSell, double fixedBuy) {
			this.fixedSell = fixedSell;
			this.fixedBuy = fixedBuy;
		}
		public double getFixedSell() {
			return fixedSell;
		}
		public double getFixedBuy() {
			return fixedBuy;
		}
	}
}
