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

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.PropertiesBinance;
import com.jbescos.exchange.PropertiesKucoin;
import com.jbescos.exchange.PropertiesMizar;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.News;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;

public class CloudProperties implements PropertiesBinance, PropertiesKucoin, PropertiesMizar {

    private static final Logger LOGGER = Logger.getLogger(CloudProperties.class.getName());
    public static final String PROPERTIES_FILE = "cloud.properties";
    public final String PROPERTIES_BUCKET;
    public final String BUCKET;
    public final String USER_ID;
    public final String EMAIL;
    public final String BOT_HOME_PAGE;
    public final String BROKER_COMMISSION;
    public final boolean USER_ACTIVE;
    public final String GOOGLE_TOPIC_ID;
    public final String PROJECT_ID;
    public final Exchange USER_EXCHANGE;
    public final String BINANCE_PUBLIC_KEY;
    public final String BINANCE_PRIVATE_KEY;
    public final String KUCOIN_PUBLIC_KEY;
    public final String KUCOIN_PRIVATE_KEY;
    public final String KUCOIN_API_PASSPHRASE;
    public final String KUCOIN_API_VERSION;
    public final String COMMAS_PUBLIC_KEY;
    public final String COMMAS_PRIVATE_KEY;
    public final String COMMAS_ACCOUNT_ID;
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
    private final Map<String, Double> MIN_SELL;
    public final boolean BOT_BUY_IGNORE_FACTOR_REDUCER;
    public final boolean LIMITS_BROKER_ENABLE;
    public final double BOT_MAX_PROFIT_SELL;
    public final double BOT_MIN_PROFIT_SELL;
    public final double BOT_PROFIT_DAYS_SUBSTRACTOR;
    public final double BOT_LOWEST_ALLOWED_PROFIT_SELL;
    public final int MAX_OPEN_POSITIONS;
    public final int MAX_OPEN_POSITIONS_SYMBOLS;
    public final int MAX_PURCHASES_PER_ITERATION;
    public final double BOT_LIMITS_FACTOR_MULTIPLIER;
    public final double BOT_LIMITS_FACTOR_PROFIT_SELL;
    public final double BOT_DCA_RATIO_BUY;
    public final String TELEGRAM_BOT_TOKEN;
    public final String TELEGRAM_CHAT_REPORT_ID;
    public final String TELEGRAM_CHAT_ID;
    public final String CHART_URL;
    public final Map<String, Double> FIXED_BUY;
    public final Map<String, Double> FIXED_SELL;
    private final Properties mainProperties;
    private final Properties idProperties;

    public CloudProperties(StorageInfo storageInfo) {
        this(null, storageInfo, null, null);
    }
    
    public CloudProperties(String userId, StorageInfo storageInfo) {
        this(userId, storageInfo, null, null);
    }
    
    public CloudProperties(String userId, Properties mainProp, Properties idProp) {
        this(userId, null, mainProp, idProp);
    }

    private CloudProperties(String userId, StorageInfo storageInfo, Properties mainProp, Properties idProp) {
        USER_ID = userId;
        if (mainProp == null && idProp == null) {
            try {
                Properties mainProperties = null;
                if (storageInfo != null) {
                    // Cloud
                    idProperties = new Properties();
                    PROJECT_ID = storageInfo.getProjectId();
                    mainProperties = new Properties();
                    BUCKET = storageInfo.getBucket();
                    PROPERTIES_BUCKET = storageInfo.getPropertiesBucket();
                    loadProperties(PROPERTIES_BUCKET, mainProperties, storageInfo.getStorage(), PROPERTIES_FILE);
                    if (USER_ID != null) {
                        loadProperties(PROPERTIES_BUCKET, idProperties, storageInfo.getStorage(),
                                USER_ID + "/" + PROPERTIES_FILE);
                    }
                } else {
                    mainProperties = Utils.fromClasspath("/" + PROPERTIES_FILE);
                    PROJECT_ID = "test";
                    PROPERTIES_BUCKET = "";
                    BUCKET = "";
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
        } else {
            this.mainProperties = mainProp;
            this.idProperties = idProp;
            PROJECT_ID = "local-bot";
            PROPERTIES_BUCKET = "";
            BUCKET = "";
        }
        USER_ACTIVE = Boolean.valueOf(getProperty("user.active"));

        TELEGRAM_BOT_TOKEN = getProperty("telegram.bot.token");
        TELEGRAM_CHAT_REPORT_ID = getProperty("telegram.chat.report.id");
        TELEGRAM_CHAT_ID = getProperty("telegram.chat.id");
        CHART_URL = getProperty("chart.url");

        EMAIL = getProperty("user.email");
        GOOGLE_TOPIC_ID = getProperty("google.topic.id");
        USER_EXCHANGE = Exchange.valueOf(getProperty("user.exchange"));
//        LOGGER.info(() -> "UserId: " + userId + ", ProjectId = " + PROJECT_ID + ", Exchange = " + USER_EXCHANGE.name());
        BINANCE_PUBLIC_KEY = getProperty("binance.public.key");
        BINANCE_PRIVATE_KEY = getProperty("binance.private.key");
        KUCOIN_PUBLIC_KEY = getProperty("kucoin.public.key");
        KUCOIN_PRIVATE_KEY = getProperty("kucoin.private.key");
        KUCOIN_API_PASSPHRASE = getProperty("kucoin.passphrase.key");
        KUCOIN_API_VERSION = getProperty("kucoin.version");
        COMMAS_PUBLIC_KEY = getProperty("3commas.public.key");
        COMMAS_PRIVATE_KEY = getProperty("3commas.private.key");
        COMMAS_ACCOUNT_ID = getProperty("3commas.account.id");
        MIZAR_API_KEY = getProperty("mizar.api.key");
        MIZAR_STRATEGY_ID = Integer.parseInt(getProperty("mizar.strategy.id"));
        LIMIT_TRANSACTION_RATIO_AMOUNT = Double.parseDouble(getProperty("limit.transaction.ratio.amount"));
        LIMIT_TRANSACTION_AMOUNT = Double.parseDouble(getProperty("limit.transaction.amount"));
        String value = getProperty("bot.white.list");
        BOT_WHITE_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() : Arrays.asList(value.split(","));
        value = getProperty("bot.never.buy");
        BOT_NEVER_BUY_LIST_SYMBOLS = "".equals(value) ? Collections.emptyList() : Arrays.asList(value.split(","));
        BROKER_COMMISSION = getProperty("broker.commission");
        BOT_BUY_REDUCER = Double.parseDouble(getProperty("bot.buy.reducer"));
        BOT_PERCENTILE_BUY_FACTOR = Double.parseDouble(getProperty("bot.percentile.buy.factor"));
        BOT_BUY_COMMISSION = Double.parseDouble(getProperty("bot.buy.comission"));
        BOT_SELL_COMMISSION = Double.parseDouble(getProperty("bot.sell.comission"));
        BOT_MIN_MAX_RELATION_BUY = Double.parseDouble(getProperty("bot.min.max.relation.buy"));
        BOT_HOURS_BACK_STATISTICS = Integer.parseInt(getProperty("bot.hours.back.statistics"));
        MIN_TRANSACTION = Double.parseDouble(getProperty("min.transaction"));
        BOT_BUY_IGNORE_FACTOR_REDUCER = Boolean.valueOf(getProperty("bot.buy.ignore.factor.reducer"));
        BOT_MAX_PROFIT_SELL = Double.parseDouble(getProperty("bot.max.profit.sell"));
        BOT_MIN_PROFIT_SELL = Double.parseDouble(getProperty("bot.min.profit.sell"));
        MAX_OPEN_POSITIONS = Integer.parseInt(getProperty("bot.max.open.positions"));
        MAX_OPEN_POSITIONS_SYMBOLS = Integer.parseInt(getProperty("bot.max.open.positions.symbols"));
        MAX_PURCHASES_PER_ITERATION = Integer.parseInt(getProperty("bot.max.purchases.per.iteration"));
        LIMITS_BROKER_ENABLE = Boolean.valueOf(getProperty("bot.limits.enable"));
        BOT_LOWEST_ALLOWED_PROFIT_SELL = Double.parseDouble(getProperty("bot.lowest.allowed.profit.sell"));
        BOT_PROFIT_DAYS_SUBSTRACTOR = Double.parseDouble(getProperty("bot.profit.days.substractor"));
        BOT_LIMITS_FACTOR_MULTIPLIER = Double.parseDouble(getProperty("bot.limits.factor.multiplier"));
        BOT_LIMITS_FACTOR_PROFIT_SELL = Double.parseDouble(getProperty("bot.limits.factor.profit.sell"));
        BOT_DCA_RATIO_BUY = Double.parseDouble(getProperty("bot.dca.ratio.buy"));
        BOT_HOME_PAGE = getProperty("bot.home.page");
        Map<String, Double> minSell = createMinSell(idProperties);
        if (minSell.isEmpty()) {
            minSell = createMinSell(mainProperties);
        }
        MIN_SELL = minSell;
        Map<String, Double> fixedBuy = fixedLimits(idProperties, "bot.limits.fixed.buy.");
        if (fixedBuy.isEmpty()) {
            fixedBuy = fixedLimits(mainProperties, "bot.limits.fixed.buy.");
        }
        FIXED_BUY = fixedBuy;
        Map<String, Double> fixedSell = fixedLimits(idProperties, "bot.limits.fixed.sell.");
        if (fixedSell.isEmpty()) {
            fixedSell = fixedLimits(mainProperties, "bot.limits.fixed.sell.");
        }
        FIXED_SELL = fixedSell;
    }

    private String getProperty(String name) {
        String value = idProperties.getProperty(name);
        if (value == null) {
            value = mainProperties.getProperty(name);
            if (value == null) {
                throw new PropertiesConfigurationException("Property " + name + " was not found in properties files",
                        new TelegramInfo(USER_ID, TELEGRAM_BOT_TOKEN,
                                TELEGRAM_CHAT_REPORT_ID, TELEGRAM_CHAT_ID, CHART_URL));
            }
        }
        return value;
    }

    private void loadProperties(String bucket, Properties properties, Storage storage, String propertiesFile)
            throws IOException {
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
            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
            properties.load(inputStream);
        }
    }

    private Map<String, Double> createMinSell(Properties properties) {
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

    private Map<String, Double> fixedLimits(Properties properties, String limitBuyOrSellKey) {
        Map<String, Double> fixedLimits = new HashMap<>();
        if (LIMITS_BROKER_ENABLE) {
            @SuppressWarnings("unchecked")
            Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();
            while (enums.hasMoreElements()) {
                String key = enums.nextElement();
                if (key.startsWith(limitBuyOrSellKey)) {
                    String symbol = key.split("\\.")[4];
                    if (!fixedLimits.containsKey(symbol)) {
                        String value = properties.getProperty(limitBuyOrSellKey + symbol);
                        if (value != null) {
                            double limit = Double.parseDouble(value);
                            fixedLimits.put(symbol, limit);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableMap(fixedLimits);
    }

    public double minSell(String symbol) {
        Double value = MIN_SELL.get(symbol);
        return value == null ? 0.0 : value;
    }

    public static enum Exchange {
        BINANCE("/binance/", true, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredBinanceAPI.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceBinance();
            }

            @Override
            public List<News> news(PublicAPI publicApi, long fromTimestamp) {
                return publicApi.delistedBinance(fromTimestamp);
            }
        },
        KUCOIN("/kucoin/", true, true) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredKucoinAPI.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceKucoin();
            }

            @Override
            public List<News> news(PublicAPI publicApi, long fromTimestamp) {
                return publicApi.delistedKucoin(fromTimestamp);
            }
        },
        MIZAR_KUCOIN("/kucoin/", false, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarAPI.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceKucoin();
            }
        },
        MIZAR_OKEX("/okex/", false, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarAPI.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceOkex();
            }
        },
        MIZAR_BINANCE("/binance/", false, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarAPI.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceBinance();
            }
        },
        MIZAR_DCA_KUCOIN("/kucoin/", false, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarDCA.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceKucoin();
            }
        },
        MIZAR_DCA_BINANCE("/binance/", false, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return SecuredMizarDCA.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceBinance();
            }
        },
        COMMAS_KUCOIN("/kucoin/", false, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return Secured3CommasAPI.create(cloudProperties, client);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceKucoin();
            }
        },
        OKEX("/okex/", true, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                throw new UnsupportedOperationException("OKEX integration is not supported");
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceOkex();
            }
        },
        CHAIN_ETHEREUM("/chain_ethereum/", true, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                throw new UnsupportedOperationException("CHAIN_ETHEREUM integration is not supported");
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceCoingeckoTopSimple(5, "ethereum");
            }
        },
        TEST_KUCOIN("/kucoin/", true, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return new SecuredTestAPI(cloudProperties, storage);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceKucoin();
            }
        },
        TEST_BINANCE("/binance/", true, false) {
            @Override
            public SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                    throws KeyException, IOException, NoSuchAlgorithmException {
                return new SecuredTestAPI(cloudProperties, storage);
            }

            @Override
            public Map<String, Price> price(PublicAPI publicApi) {
                return publicApi.priceBinance();
            }
        };

        private final String folder;
        private final boolean supportWallet;
        private final boolean supportSyncTransaction;

        private Exchange(String folder, boolean supportWallet, boolean supportSyncTransaction) {
            this.folder = folder;
            this.supportWallet = supportWallet;
            this.supportSyncTransaction = supportSyncTransaction;
        }

        public boolean isSupportWallet() {
            return supportWallet;
        }

        public boolean isSupportSyncTransaction() {
            return supportSyncTransaction;
        }

        public String getFolder() {
            return folder;
        }

        public boolean enabled() {
            return true;
        }

        public List<News> news(PublicAPI publicApi, long fromTimestamp) {
            return Collections.emptyList();
        }

        public abstract SecuredAPI create(CloudProperties cloudProperties, Client client, FileManager storage)
                throws KeyException, IOException, NoSuchAlgorithmException;

        public abstract Map<String, Price> price(PublicAPI publicApi);

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
        return KUCOIN_API_PASSPHRASE;
    }

    @Override
    public String kucoinApiVersion() {
        return KUCOIN_API_VERSION;
    }

    @Override
    public double kucoinBuyCommission() {
        return BOT_BUY_COMMISSION;
    }

    @Override
    public double kucoinSellCommission() {
        return BOT_SELL_COMMISSION;
    }

    @Override
    public List<String> mizarWhiteListSymbols() {
        return BOT_WHITE_LIST_SYMBOLS;
    }

    @Override
    public int mizarStrategyId() {
        return MIZAR_STRATEGY_ID;
    }

    @Override
    public String mizarApiKey() {
        return MIZAR_API_KEY;
    }

    @Override
    public double mizarLimitTransactionAmount() {
        return LIMIT_TRANSACTION_AMOUNT;
    }

    @Override
    public boolean mizarBuyIgnoreFactorReducer() {
        return BOT_BUY_IGNORE_FACTOR_REDUCER;
    }
}
