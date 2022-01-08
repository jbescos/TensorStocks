package com.jbescos.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.StorageOptions;

public class CloudProperties {

    private static final Logger LOGGER = Logger.getLogger(CloudProperties.class.getName());
    private static final String PREFIX_PROPERTIES_BUCKET = "crypto-properties";
    private static final String PREFIX_STORAGE_BUCKET = "crypto-for-training";
    public final String PROPERTIES_BUCKET;
    public final String BUCKET;
    public final String USER_ID;
    public final String EMAIL;
    public final String BROKER_COMMISSION;
    public final boolean USER_ACTIVE;
    private final String PROPERTIES_FILE = "cloud.properties";
    public final String GOOGLE_TOPIC_ID;
    public final String PROJECT_ID;
    public final Exchange USER_EXCHANGE;
    public final String BINANCE_PUBLIC_KEY;
    public final String BINANCE_PRIVATE_KEY;
    public final String KUCOIN_PUBLIC_KEY;
    public final String KUCOIN_PRIVATE_KEY;
    public final String KUCOIN_API_PASSPHRASE;
    public final String KUCOIN_COMMERCE_KEY;
    public final String KUCOIN_API_VERSION;
    public final String MIZAR_API_KEY;
    public final int MIZAR_STRATEGY_ID;
    public final double LIMIT_TRANSACTION_AMOUNT;
    public final double LIMIT_TRANSACTION_RATIO_AMOUNT;
    public final double MIN_TRANSACTION;
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
    public final boolean LIMITS_BROKER_ENABLE;
    public final double BOT_MAX_PROFIT_SELL;
    public final double BOT_PANIC_FACTOR;
    public final double BOT_PANIC_RATIO;
    public final double BOT_MIN_PROFIT_SELL;
    public final double BOT_PROFIT_DAYS_SUBSTRACTOR;
    public final double BOT_LOWEST_ALLOWED_PROFIT_SELL;
    public final int BOT_PANIC_DAYS;
    public final int MAX_OPEN_POSITIONS;
    public final int MAX_OPEN_POSITIONS_SYMBOLS;
    public final int MAX_PURCHASES_PER_ITERATION;
    public final double BOT_LIMITS_FACTOR_MULTIPLIER;
    public final double BOT_LIMITS_FACTOR_PROFIT_SELL;
    public final Map<String, Double> FIXED_BUY;
    private final Properties mainProperties;
    private final Properties idProperties;
    
    public CloudProperties() {
        this(null);
    }

    public CloudProperties(String userId) {
        USER_ID = userId;
        try {
            Properties mainProperties = Utils.fromClasspath("/" + PROPERTIES_FILE);
            if (mainProperties == null) {
            	idProperties = new Properties();
                PROJECT_ID = StorageOptions.getDefaultProjectId();
                mainProperties = new Properties();
                Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
                BUCKET = findByPrefix(storage, PREFIX_STORAGE_BUCKET);
                if (BUCKET == null) {
                    throw new IllegalStateException("Bucket that starts with " + PREFIX_STORAGE_BUCKET + " was not found");
                }
                PROPERTIES_BUCKET = findByPrefix(storage, PREFIX_PROPERTIES_BUCKET);
                if (PROPERTIES_BUCKET == null) {
                    throw new IllegalStateException("Bucket that starts with " + PREFIX_PROPERTIES_BUCKET + " was not found");
                }
                loadProperties(PROPERTIES_BUCKET, mainProperties, storage, PROPERTIES_FILE);
                if (USER_ID != null) {
                    loadProperties(PROPERTIES_BUCKET, idProperties, storage, USER_ID + "/" + PROPERTIES_FILE);
                }
            } else {
                PROJECT_ID = "test";
                PROPERTIES_BUCKET = PREFIX_PROPERTIES_BUCKET;
                BUCKET = PREFIX_STORAGE_BUCKET;
                if (USER_ID != null) {
                	idProperties = Utils.fromClasspath("/" + USER_ID + "/" + PROPERTIES_FILE);
                } else {
                	idProperties = new Properties();
                }
            }
            this.mainProperties = mainProperties;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load properties file", e);
        }
        USER_ACTIVE = Boolean.valueOf(getProperty("user.active"));
        EMAIL = getProperty("user.email");
        GOOGLE_TOPIC_ID = getProperty("google.topic.id");
        USER_EXCHANGE = Exchange.valueOf(getProperty("user.exchange"));
        LOGGER.info(() -> "UserId: " + userId + ", ProjectId = " + PROJECT_ID + ", Exchange = " + USER_EXCHANGE.name());
        BINANCE_PUBLIC_KEY = getProperty("binance.public.key");
        BINANCE_PRIVATE_KEY = getProperty("binance.private.key");
        KUCOIN_PUBLIC_KEY = getProperty("kucoin.public.key");
        KUCOIN_PRIVATE_KEY = getProperty("kucoin.private.key");
        KUCOIN_API_PASSPHRASE = getProperty("kucoin.passphrase.key");
        KUCOIN_COMMERCE_KEY = getProperty("kucoin.commerce.key");
        KUCOIN_API_VERSION = getProperty("kucoin.version");
        
        MIZAR_API_KEY = getProperty("mizar.api.key");
        MIZAR_STRATEGY_ID = Integer.parseInt(getProperty("mizar.strategy.id"));
        LIMIT_TRANSACTION_RATIO_AMOUNT = Double.parseDouble(getProperty("limit.transaction.ratio.amount"));
        LIMIT_TRANSACTION_AMOUNT = Double.parseDouble(getProperty("limit.transaction.amount"));
        String value = getProperty("bot.white.list");
        BOT_WHITE_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() : Arrays.asList(value.split(","));
        value = getProperty("bot.never.buy");
        BOT_NEVER_BUY_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() :  Arrays.asList(value.split(","));
        BROKER_COMMISSION = getProperty("broker.commission");
        BOT_BUY_REDUCER = Double.parseDouble(getProperty("bot.buy.reducer"));
        BOT_PERCENTILE_BUY_FACTOR = Double.parseDouble(getProperty("bot.percentile.buy.factor"));
        BOT_BUY_COMMISSION = Double.parseDouble(getProperty("bot.buy.comission"));
        BOT_SELL_COMMISSION = Double.parseDouble(getProperty("bot.sell.comission"));
        BOT_MIN_MAX_RELATION_BUY = Double.parseDouble(getProperty("bot.min.max.relation.buy"));
        BOT_HOURS_BACK_STATISTICS = Integer.parseInt(getProperty("bot.hours.back.statistics"));
        BOT_MONTHS_BACK_TRANSACTIONS = Integer.parseInt(getProperty("bot.months.back.transactions"));
        MIN_TRANSACTION = Double.parseDouble(getProperty("min.transaction"));
        EWMA_CONSTANT = Double.parseDouble(getProperty("ewma.constant"));
        EWMA_2_CONSTANT = Double.parseDouble(getProperty("ewma.2.constant"));
        BOT_BUY_IGNORE_FACTOR_REDUCER = Boolean.valueOf(getProperty("bot.buy.ignore.factor.reducer"));
        BOT_MAX_PROFIT_SELL = Double.parseDouble(getProperty("bot.max.profit.sell"));
        BOT_MIN_PROFIT_SELL = Double.parseDouble(getProperty("bot.min.profit.sell"));
        BOT_PANIC_RATIO = Double.parseDouble(getProperty("bot.panic.ratio"));
        BOT_PANIC_FACTOR = Double.parseDouble(getProperty("bot.panic.factor"));
        BOT_PANIC_DAYS = Integer.parseInt(getProperty("bot.panic.days"));
        MAX_OPEN_POSITIONS = Integer.parseInt(getProperty("bot.max.open.positions"));
        MAX_OPEN_POSITIONS_SYMBOLS = Integer.parseInt(getProperty("bot.max.open.positions.symbols"));
        MAX_PURCHASES_PER_ITERATION = Integer.parseInt(getProperty("bot.max.purchases.per.iteration"));
        PANIC_BROKER_ENABLE = Boolean.valueOf(getProperty("bot.panic.enable"));
        LIMITS_BROKER_ENABLE = Boolean.valueOf(getProperty("bot.limits.enable"));
        BOT_LOWEST_ALLOWED_PROFIT_SELL = Double.parseDouble(getProperty("bot.lowest.allowed.profit.sell"));
        BOT_PROFIT_DAYS_SUBSTRACTOR = Double.parseDouble(getProperty("bot.profit.days.substractor"));
        BOT_LIMITS_FACTOR_MULTIPLIER = Double.parseDouble(getProperty("bot.limits.factor.multiplier"));
        BOT_LIMITS_FACTOR_PROFIT_SELL = Double.parseDouble(getProperty("bot.limits.factor.profit.sell"));
        Map<String, Double> minSell = createMinSell(idProperties);
        if (minSell.isEmpty()) {
            minSell = createMinSell(mainProperties);
        }
        MIN_SELL = minSell;
        Map<String, Double> fixedBuy = fixedBuy(idProperties);
        if (fixedBuy.isEmpty()) {
            fixedBuy = fixedBuy(mainProperties);
        }
        FIXED_BUY = fixedBuy;
    }
    
    private String findByPrefix(Storage storage, String prefix) {
    	Page<Bucket> page = storage.list(BucketListOption.prefix(prefix));
        for (Bucket bucket : page.iterateAll()) {
            return bucket.getName();
        }
        return null;
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

    private void loadProperties(String bucket, Properties properties, Storage storage, String propertiesFile) throws IOException {
        Blob blob = storage.get(bucket, propertiesFile);
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
        @SuppressWarnings("unchecked")
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
    
    private  Map<String, Double> fixedBuy(Properties properties) {
        Map<String, Double> fixedBuy = new HashMap<>();
        if (LIMITS_BROKER_ENABLE) {
	        @SuppressWarnings("unchecked")
			Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();
	        while (enums.hasMoreElements()) {
	            String key = enums.nextElement();
	            if (key.startsWith("bot.limits.fixed")) {
	                String symbol = key.split("\\.")[4];
	                if (!fixedBuy.containsKey(symbol)) {
	                    double buy = Double.parseDouble(properties.getProperty("bot.limits.fixed.buy." + symbol));
	                    fixedBuy.put(symbol, buy);
	                }
	            }
	        }
        }
        return Collections.unmodifiableMap(fixedBuy);
    }

    public double minSell(String symbol) {
        Double value = MIN_SELL.get(symbol);
        return value == null ? 0.0 : value;
    }

    public static enum Exchange {
        BINANCE("/binance/", true) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredBinanceAPI.create(cloudProperties, client);
            }

			@Override
			public Map<String, Double> price(PublicAPI publicApi) {
				return publicApi.priceBinance();
			}
        }, KUCOIN("/kucoin/", true) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredKucoinAPI.create(cloudProperties, client);
            }

			@Override
			public Map<String, Double> price(PublicAPI publicApi) {
				return publicApi.priceKucoin();
			}
        }, MIZAR_KUCOIN("/kucoin/", false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarAPI.create(cloudProperties, client);
            }

			@Override
			public Map<String, Double> price(PublicAPI publicApi) {
				return publicApi.priceKucoin();
			}
        }, MIZAR_OKEX("/okex/", false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarAPI.create(cloudProperties, client);
            }

			@Override
			public Map<String, Double> price(PublicAPI publicApi) {
				return publicApi.priceOkex();
			}
        }, MIZAR_FTX("/ftx/", false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarAPI.create(cloudProperties, client);
            }

			@Override
			public Map<String, Double> price(PublicAPI publicApi) {
				return publicApi.priceFtx();
			}
        }, MIZAR_BINANCE("/binance/", false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException {
            	return SecuredMizarAPI.create(cloudProperties, client);
            }

			@Override
			public Map<String, Double> price(PublicAPI publicApi) {
				return publicApi.priceBinance();
			}
        };
    	
    	private final String folder;
    	private final boolean supportWallet;
    	
    	private Exchange(String folder, boolean supportWallet) {
    		this.folder = folder;
    		this.supportWallet = supportWallet;
    	}
        
        public boolean isSupportWallet() {
			return supportWallet;
		}

		public String getFolder() {
			return folder;
		}

		public abstract SecuredAPI create(CloudProperties cloudProperties, Client client) throws KeyException, IOException, NoSuchAlgorithmException;
        
        public abstract Map<String, Double> price(PublicAPI publicApi);

    }
}
