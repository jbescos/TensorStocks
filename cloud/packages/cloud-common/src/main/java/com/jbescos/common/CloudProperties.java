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

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class CloudProperties {

	private static final Logger LOGGER = Logger.getLogger(CloudProperties.class.getName());
	public final String USER_ID;
	private final boolean USER_ACTIVE;
	public final String PROPERTIES_BUCKET = "crypto-properties";
	private final String PROPERTIES_FILE = "cloud.properties";
	public final String PROJECT_ID = System.getenv("GCP_PROJECT");
	public final String BUCKET;
	public final String GOOGLE_TOPIC_ID;
	public final String GOOGLE_PROJECT_ID;
	public final String BINANCE_PUBLIC_KEY;
	public final String BINANCE_PRIVATE_KEY;
	public final double BINANCE_MIN_TRANSACTION;
	public final List<String> BOT_NEVER_BUY_LIST_SYMBOLS;
	public final List<String> BOT_WHITE_LIST_SYMBOLS;
	public final double BOT_BUY_REDUCER;
	public final double BOT_PERCENTILE_BUY_FACTOR;
	public final double BOT_BUY_COMMISSION;
	public final double BOT_SELL_COMMISSION;
	public final double BOT_MIN_MAX_RELATION_BUY;
	public final int BOT_HOURS_BACK_STATISTICS;
	public final int BOT_MONTHS_BACK_TRANSACTIONS;
	private final Map<String, Double> MIN_SELL;
	public final double EWMA_CONSTANT;
	public final double EWMA_2_CONSTANT;
	public final boolean BOT_BUY_IGNORE_FACTOR_REDUCER;
	public final boolean PANIC_BROKER_ENABLE;
	public final boolean GREEDY_BROKER_ENABLE;
	public final double BOT_MAX_PROFIT_SELL;
	public final double BOT_PANIC_RATIO;
	public final double BOT_MIN_PROFIT_SELL;
	public final double BOT_PROFIT_DAYS_SUBSTRACTOR;
	public final int BOT_PANIC_DAYS;
	public final double BOT_GREEDY_MIN_PERCENTILE_BUY;
	public final double BOT_GREEDY_MIN_PROFIT_SELL;
	public final double BOT_GREEDY_DEFAULT_FACTOR_SELL;
	public final int BOT_GREEDY_DAYS_TO_HOLD;
	public final double BOT_GREEDY_IMMEDIATELY_SELL;
	public final double BOT_GREEDY_MIN_MAX_RELATION_BUY;
	public final Map<String, FixedBuySell> FIXED_BUY_SELL;
	private final Properties mainProperties;
	private final Properties idProperties = new Properties();
	
	public CloudProperties() {
		this(null);
	}

	public CloudProperties(String userId) {
		USER_ID = userId;
		try {
			Properties mainProperties = Utils.fromClasspath("/" + PROPERTIES_FILE);
			if (mainProperties == null) {
				mainProperties = new Properties();
				Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
				loadProperties(mainProperties, storage, PROPERTIES_FILE);
				if (USER_ID != null) {
					loadProperties(idProperties, storage, USER_ID + "/" + PROPERTIES_FILE);
				}
			}
			this.mainProperties = mainProperties;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load properties file", e);
		}
		USER_ACTIVE = Boolean.valueOf(getProperty("user.active"));
		if (!USER_ACTIVE) {
			throw new IllegalStateException("User ID " + USER_ID + " is not active");
		}
		
		BUCKET = getProperty("storage.bucket");
		GOOGLE_TOPIC_ID = getProperty("google.topic.id");
		GOOGLE_PROJECT_ID = getProperty("google.project.id");
		BINANCE_PUBLIC_KEY = getProperty("binance.public.key");
		BINANCE_PRIVATE_KEY = getProperty("binance.private.key");
		String value = getProperty("bot.white.list");
		BOT_WHITE_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() : Arrays.asList(value.split(","));
		value = getProperty("bot.never.buy");
		BOT_NEVER_BUY_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() :  Arrays.asList(value.split(","));
		BOT_BUY_REDUCER = Double.parseDouble(getProperty("bot.buy.reducer"));
		BOT_PERCENTILE_BUY_FACTOR = Double.parseDouble(getProperty("bot.percentile.buy.factor"));
		BOT_BUY_COMMISSION = Double.parseDouble(getProperty("bot.buy.comission"));
		BOT_SELL_COMMISSION = Double.parseDouble(getProperty("bot.sell.comission"));
		BOT_MIN_MAX_RELATION_BUY = Double.parseDouble(getProperty("bot.min.max.relation.buy"));
		BOT_HOURS_BACK_STATISTICS = Integer.parseInt(getProperty("bot.hours.back.statistics"));
		BOT_MONTHS_BACK_TRANSACTIONS = Integer.parseInt(getProperty("bot.months.back.transactions"));
		BINANCE_MIN_TRANSACTION = Double.parseDouble(getProperty("binance.min.transaction"));
		EWMA_CONSTANT = Double.parseDouble(getProperty("ewma.constant"));
		EWMA_2_CONSTANT = Double.parseDouble(getProperty("ewma.2.constant"));
        BOT_BUY_IGNORE_FACTOR_REDUCER = Boolean.valueOf(getProperty("bot.buy.ignore.factor.reducer"));
        BOT_MAX_PROFIT_SELL = Double.parseDouble(getProperty("bot.max.profit.sell"));
        BOT_MIN_PROFIT_SELL = Double.parseDouble(getProperty("bot.min.profit.sell"));
        BOT_PANIC_RATIO = Double.parseDouble(getProperty("bot.panic.ratio"));
        BOT_PANIC_DAYS = Integer.parseInt(getProperty("bot.panic.days"));
        BOT_GREEDY_MIN_PERCENTILE_BUY = Double.parseDouble(getProperty("bot.greedy.min.percentile.buy"));
        BOT_GREEDY_MIN_PROFIT_SELL = Double.parseDouble(getProperty("bot.greedy.min.profit.sell"));
        PANIC_BROKER_ENABLE = Boolean.valueOf(getProperty("bot.panic.enable"));
        GREEDY_BROKER_ENABLE = Boolean.valueOf(getProperty("bot.greedy.enable"));
        BOT_GREEDY_DEFAULT_FACTOR_SELL = Double.parseDouble(getProperty("bot.greedy.default.factor.to.sell"));
        BOT_GREEDY_DAYS_TO_HOLD = Integer.parseInt(getProperty("bot.greedy.days.to.hold"));
        BOT_GREEDY_IMMEDIATELY_SELL = Double.parseDouble(getProperty("bot.greedy.immediately.sell"));
        BOT_GREEDY_MIN_MAX_RELATION_BUY = Double.parseDouble(getProperty("bot.greedy.min.max.relation.buy"));
        BOT_PROFIT_DAYS_SUBSTRACTOR = Double.parseDouble(getProperty("bot.profit.days.substractor"));
        Map<String, Double> minSell = createMinSell(idProperties);
        if (minSell.isEmpty()) {
        	minSell = createMinSell(mainProperties);
        }
        MIN_SELL = minSell;
        Map<String, FixedBuySell> fixedBuySell = fixedBuySell(idProperties);
        if (fixedBuySell.isEmpty()) {
        	fixedBuySell = fixedBuySell(mainProperties);
        }
        FIXED_BUY_SELL = fixedBuySell;
    }
	
	private String getProperty(String name) {
		String value = idProperties.getProperty(name);
		if (value == null) {
			value = mainProperties.getProperty(name);
			if (value == null) {
				throw new IllegalStateException("Property " + name + " was not found in properties files");
			}
		}
		return value;
	}

	private void loadProperties(Properties properties, Storage storage, String propertiesFile) throws IOException {
		Blob blob = storage.get(PROPERTIES_BUCKET, propertiesFile);
		if (blob == null) {
			throw new IllegalStateException("There is no " + propertiesFile);
		}
		try (ReadChannel readChannel = blob.reader();
				BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\r\n");
			}
			InputStream inputStream = new ByteArrayInputStream(
					builder.toString().getBytes(StandardCharsets.UTF_8));
			properties.load(inputStream);
		}
	}

    private  Map<String, Double> createMinSell(Properties properties) {
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
	
    private  Map<String, FixedBuySell> fixedBuySell(Properties properties) {
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

    public double minSell(String symbol) {
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
