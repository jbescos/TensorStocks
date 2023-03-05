package com.jbescos.exchange;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.AllTickers.Ticker;

public final class PublicAPI {

    private static final Logger LOGGER = Logger.getLogger(PublicAPI.class.getName());
    private static final String FEAR_GREEDY_URL = "https://api.alternative.me/fng/";
    private static final String BINANCE_URL = "https://api.binance.com";
//    private static final String BINANCE_URL = "https://api.binance.us";
    private static final String KUCOIN_URL = "https://api.kucoin.com";
    private static final String KUCOIN_NEWS_API = "https://www.kucoin.com";
    private static final String KUCOIN_NEWS_PAGE = KUCOIN_NEWS_API + "/news";
    // TODO
    private static final String BINANCE_NEWS_API = "https://www.binance.com/bapi/composite/v1";
    // https://www.binance.com/en/support/announcement/${title}-${code}
    // https://www.binance.com/en/support/announcement/notice-of-removal-of-trading-pairs-2022-07-07-c99bd249484f4bbaa7f8489ae2bc860a
    private static final String OKEX_URL = "https://www.okx.com/";
    private static final String FTX_URL = "https://ftx.com";
    private static final String COINGEKO_URL = "https://api.coingecko.com";
    // https://mxcdevelop.github.io/APIDoc/
    private static final String MEXC_URL = "https://www.mexc.com";
    // https://bybit-exchange.github.io/docs/inverse
    private static final String BYBIT_URL = "https://api.bybit.com";
    // https://huobiapi.github.io/docs/spot/v1/en
    private static final String HUOBI_URL = "https://api-aws.huobi.pro";
    // https://woocommerce.github.io/woocommerce-rest-api-docs
    private static final String WOO_URL = "todo";
    // https://exchange-docs.crypto.com/spot/index.html
    private static final String CRYPTOCOM_URL = "https://api.crypto.com/v2";
    private final Client client;

    public PublicAPI(Client client) {
        this.client = client;
    }

    public long time() {
        return get(BINANCE_URL, "/api/v3/time", new GenericType<ServerTime>() {
        }).getServerTime();
    }

    public Map<String, Object> exchangeInfoFilter(String symbol, String filterName) {
        String[] query = null;
        if (symbol != null) {
            query = new String[] { "symbol", symbol };
        } else {
            query = new String[0];
        }
        Map<String, Object> exchangeInfo = get(BINANCE_URL, "/api/v3/exchangeInfo",
                new GenericType<Map<String, Object>>() {
                }, query);
        List<Map<String, Object>> symbolFilters = (List<Map<String, Object>>) exchangeInfo.get("symbols");
        return getBinanceFilter(symbol, filterName, symbolFilters);
    }

    private Map<String, Object> getBinanceFilter(String symbol, String filterName,
            List<Map<String, Object>> symbolFilters) {
        for (Map<String, Object> symbolFilter : symbolFilters) {
            if (symbol.equals(symbolFilter.get("symbol"))) {
                List<Map<String, Object>> filters = (List<Map<String, Object>>) symbolFilter.get("filters");
                for (Map<String, Object> filter : filters) {
                    Object obj = filter.get("filterType");
                    if (obj != null && obj.equals(filterName)) {
                        return filter;
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    public List<FearGreedIndex> getFearGreedIndex(String days) {
        List<FearGreedIndex> list = new ArrayList<>();
        try {
            Map<String, Object> result = get(FEAR_GREEDY_URL, "", new GenericType<Map<String, Object>>() {
            }, "limit", days);
            List<Map<String, String>> data = (List<Map<String, String>>) result.get("data");
            for (Map<String, String> obj : data) {
                FearGreedIndex fearGreed = new FearGreedIndex(obj.get("value_classification"),
                        Integer.parseInt(obj.get("value")), Long.parseLong(obj.get("timestamp")) * 1000);
                list.add(fearGreed);
            }
        } catch (Exception e) {
            FearGreedIndex fearGreed = new FearGreedIndex("Error", 50, new Date().getTime());
            LOGGER.log(Level.SEVERE, "Cannot obtain the FearGreedIndex. Setting default value to " + fearGreed, e);
            list.add(fearGreed);
        }
        return list;
    }

    public Map<String, Price> priceBinance() {
        List<Map<String, Object>> prices = get(BINANCE_URL, "/api/v3/ticker/price",
                new GenericType<List<Map<String, Object>>>() {
                });
        Map<String, Price> pricesBySymbol = new HashMap<>();
        prices.stream().filter(price -> {
            String symbol = (String) price.get("symbol");
            return symbol.endsWith(Utils.USDT);
        }).forEach(price -> pricesBySymbol.put((String) price.get("symbol"),
                new Price((String) price.get("symbol"), Double.parseDouble((String) price.get("price")), "")));
        return pricesBySymbol;
    }

    public Map<String, Price> priceKucoin() {
        return priceKucoin(ticker -> Double.parseDouble(ticker.getBuy()));
    }

    public Map<String, Price> priceKucoin(Function<Ticker, Double> price) {
        AllTickers allTickers = get(KUCOIN_URL, "/api/v1/market/allTickers", new GenericType<AllTickers>() {
        });
        Map<String, Price> pricesBySymbol = new HashMap<>();
        allTickers.getData().getTicker().stream().filter(ticker -> ticker.getSymbol().endsWith(Utils.USDT))
                .filter(ticker -> ticker.getBuy() != null).forEach(ticker -> {
                    Double priceVal = price.apply(ticker);
                    if (priceVal != null) {
                        String symbol = ticker.getSymbol().replaceFirst("-", "");
                        pricesBySymbol.put(symbol, new Price(symbol, priceVal, ""));
                    }
                });
        return pricesBySymbol;
    }

    public Map<String, Price> priceOkex() {
        Map<String, Price> pricesBySymbol = new HashMap<>();
        List<Map<String, String>> data = (List<Map<String, String>>) get(OKEX_URL, "/api/v5/market/tickers",
                new GenericType<Map<String, Object>>() {
                }, "instType", "SPOT").get("data");
        data.stream().filter(ticker -> ticker.get("instId").endsWith(Utils.USDT)).forEach(ticker -> {
            Double value = Double.parseDouble(ticker.get("last"));
            if (value != null) {
                String symbol = ticker.get("instId").replaceFirst("-", "");
                pricesBySymbol.put(symbol, new Price(symbol, value, ""));
            }
        });
        return pricesBySymbol;
    }

    public Map<String, Price> priceFtx() {
        Map<String, Price> pricesBySymbol = new HashMap<>();
        get(FTX_URL, "/api/markets", new GenericType<FtxResult<List<Map<String, String>>>>() {
        }).getResult().stream().filter(ticker -> ticker.get("type").equals("spot"))
                .filter(ticker -> ticker.get("quoteCurrency").equals(Utils.USDT))
                .filter(ticker -> ticker.get("name") != null).filter(ticker -> ticker.get("price") != null)
                .filter(ticker -> !ticker.get("name").endsWith("BULL/" + Utils.USDT))
                .filter(ticker -> !ticker.get("name").endsWith("BEAR/" + Utils.USDT)).forEach(ticker -> {
                    String symbol = ticker.get("name").replaceFirst("/", "");
                    double value = Double.parseDouble(ticker.get("price"));
                    pricesBySymbol.put(symbol, new Price(symbol, value, ""));
                });
        return pricesBySymbol;
    }

    public List<Kline> klines(Interval interval, String symbol, Integer limit, long startTime, Long endTime) {
        List<String> queryParams = new ArrayList<>();
        queryParams.add("interval");
        queryParams.add(interval.value);
        queryParams.add("symbol");
        queryParams.add(symbol);
        if (limit != null) {
            queryParams.add("limit");
            queryParams.add(Integer.toString(limit));
        }
        queryParams.add("startTime");
        queryParams.add(Long.toString(startTime));
        if (endTime != null) {
            queryParams.add("endTime");
            queryParams.add(Long.toString(endTime));
        }
        List<Object[]> result = get(BINANCE_URL, "/api/v3/klines", new GenericType<List<Object[]>>() {
        }, queryParams.toArray(new String[0]));
        List<Kline> klines = new ArrayList<>(result.size());
        for (Object[] values : result) {
            klines.add(Kline.fromArray(symbol, values));
        }
        return klines;
    }

    public Map<String, Map<String, Object>> priceCoingecko(String platform) {
        Map<String, Map<String, Object>> coinsById = getCoinsByPlatform(platform);
        List<String> ids = new ArrayList<>(coinsById.keySet());
        // Avoid the URL is too big to fail
        final int LIST_LIMIT = 180;
        int i = 0;
        do {
            int to = i + LIST_LIMIT;
            if (to > ids.size()) {
                to = ids.size();
            }
            List<String> subIds = ids.subList(i, to);
            String key = String.join(",", subIds);
            Map<String, Map<String, Object>> prices = get(COINGEKO_URL, "/api/v3/simple/price",
                    new GenericType<Map<String, Map<String, Object>>>() {
                    }, "precision", "full", "vs_currencies", "usd", "ids", key);
            for (Entry<String, Map<String, Object>> price : prices.entrySet()) {
                String id = price.getKey();
                Object usd = price.getValue().get("usd");
                coinsById.get(id).put("price", usd);
            }
            i = to;
        } while (i < ids.size());
        return coinsById;
    }

    public Map<String, Map<String, Object>> priceCoingeckoTop(int pages, String platform) {
        Map<String, Map<String, Object>> filteredCoins = new HashMap<>();
        Map<String, Map<String, Object>> coinsById = getCoinsByPlatform(platform);
        int pageIdx = 0;
        List<Map<String, Object>> page = null;
        do {
            page = get(COINGEKO_URL, "/api/v3/coins/markets", new GenericType<List<Map<String, Object>>>() {
            }, "sparkline", "false", "order", "market_cap_desc", "vs_currency", "usd", "per_page", "250", "page",
                    Integer.toString(pageIdx));
            for (Map<String, Object> price : page) {
                String id = (String) price.get("id");
                Map<String, Object> coinInfo = coinsById.get(id);
                if (coinInfo != null) {
                    coinInfo.put("price", ((Number) price.get("current_price")).doubleValue());
                    filteredCoins.put(id, coinInfo);
                }
            }
            pageIdx++;
        } while (pageIdx < pages && page != null);
        return filteredCoins;
    }

    public Map<String, Price> priceCoingeckoTopSimple(int pages, String platform) {
        Map<String, Map<String, Object>> prices = priceCoingeckoTop(pages, platform);
        Map<String, Price> simple = new HashMap<>();
        for (Map<String, Object> price : prices.values()) {
            String symbol = price.get("id") + Utils.USDT;
            simple.put(symbol, new Price(symbol, (double) price.get("price"), (String) price.get("token")));
        }
        return simple;
    }

    private Map<String, Map<String, Object>> getCoinsByPlatform(String platform) {
        List<Map<String, Object>> coins = get(COINGEKO_URL, "/api/v3/coins/list",
                new GenericType<List<Map<String, Object>>>() {
                }, "include_platform", "true");
        // For performance, keep it as a HashMap
        Map<String, Map<String, Object>> coinsById = new HashMap<>();
        for (Map<String, Object> coin : coins) {
            // Get only symbols that exists in ethereum
            Map<String, String> platforms = (Map<String, String>) coin.get("platforms");
            String token = platforms.get(platform);
            if (token != null && !token.isEmpty()) {
                String id = (String) coin.get("id");
                coin.put("token", token);
                coinsById.put(id, coin);
            }
        }
        return coinsById;
    }

    public boolean isSymbolBinanceEnabled(String symbol) {
        Map<String, Object> exchangeInfo = get(BINANCE_URL, "/api/v3/exchangeInfo",
                new GenericType<Map<String, Object>>() {
                }, "symbol", symbol);
        Map<String, Object> symbolData = ((List<Map<String, Object>>) exchangeInfo.get("symbols")).get(0);
        return (boolean) symbolData.get("isSpotTradingAllowed");
    }

    public boolean isSymbolKucoinEnabled(String symbol) {
        KucoinResponse<Map<String, Object>> result = get(KUCOIN_URL,
                "/api/v1/currencies/" + symbol.replaceFirst(Utils.USDT, ""),
                new GenericType<KucoinResponse<Map<String, Object>>>() {
                });
        boolean withdraw = (boolean) result.getData().get("isWithdrawEnabled");
        boolean margin = (boolean) result.getData().get("isDepositEnabled");
        return withdraw && margin;
    }

    public boolean isSymbolEnabled(String symbol) {
        return isSymbolBinanceEnabled(symbol) && isSymbolKucoinEnabled(symbol);
    }

    public List<News> delistedKucoin(long fromTimestamp) {
        List<News> news = new ArrayList<>();
        int pageSize = 50;
        int page = 1;
        boolean done = false;
        while (!done) {
            Map<String, Object> response = get(KUCOIN_NEWS_API, "/_api/cms/articles",
                    new GenericType<Map<String, Object>>() {
                    }, "category", "announcements", "lang", "en_US", "page", Integer.toString(page), "pageSize",
                    Integer.toString(pageSize));
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            for (Map<String, Object> item : items) {
                long timestamp = 1000 * ((Number) item.get("publish_ts")).longValue();
                if (timestamp >= fromTimestamp) {
                    String title = ((String) item.get("title")).toLowerCase();
                    if (title.contains("delist")) {
                        String summary = (String) item.get("summary");
                        String url = KUCOIN_NEWS_PAGE + item.get("path");
                        News n = new News("KUCOIN", title, new Date(timestamp), summary, url);
                        addDelistedKucoin(n);
                        news.add(n);
                    }
                } else {
                    int stick = (int) item.get("stick");
                    if (stick == 0) {
                        // Sometimes they stick old time stamps
                        done = true;
                    }
                }
            }
            page++;
        }
        return news;
    }
    
    private void addDelistedKucoin(News n) {
    	int begin = n.title.indexOf("(");
    	if (begin > -1) {
    		int last = n.title.indexOf(")");
    		if (last > -1) {
    			String symbol = n.title.substring(begin + 1, last).toUpperCase() + Utils.USDT;
    			n.delistedSymbols.add(symbol);
    		}
    	}
    }
    
    public List<News> delistedBinance(long fromTimestamp) {
    	List<News> news = new ArrayList<>();
    	int pageSize = 50;
        int page = 1;
        boolean done = false;
        while (!done) {
        	Map<String, Object> response = get(BINANCE_NEWS_API, "/public/cms/article/list/query", new GenericType<Map<String, Object>>() {
            }, "type", "1", "catalogId", "161", "pageSize", Integer.toString(pageSize), "pageNo",
            Integer.toString(page));
        	List<Map<String, Object>> catalogs = (List<Map<String, Object>>) ((Map<String, Object>)response.get("data")).get("catalogs");
        	for (Map<String, Object> catalog : catalogs) {
        		List<Map<String, Object>> articles = (List<Map<String, Object>>) catalog.get("articles");
        		for (Map<String, Object> article : articles) {
        			long timestamp = (long) article.get("releaseDate");
        			 if (timestamp >= fromTimestamp) {
        				String title = (String) article.get("title");
        				String code = (String) article.get("code");
        				String encodedTitle = title;
						try {
							encodedTitle = URLEncoder.encode(title, "UTF8");
						} catch (UnsupportedEncodingException e) {}
						String url = "https://www.binance.com/en/support/announcement/" + encodedTitle + "-" + code;
        				News n = new News("BINANCE", title, new Date(timestamp), "", url);
        				news.add(n);
        			} else {
        				done = true;
        				break;
        			}
        		}
        	}
        	page++;
        }
    	
    	return news;
    }

    @SuppressWarnings("unchecked")
    public WebsocketInfo websocketInfo() {
        Map<String, Object> response = post(KUCOIN_URL, "/api/v1/bullet-public",
                new GenericType<Map<String, Object>>() {
                }, "");
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        String token = (String) data.get("token");
        List<Map<String, Object>> websockets = (List<Map<String, Object>>) data.get("instanceServers");
        String endpoint = (String) websockets.get(0).get("endpoint");
        Number pingInterval = (Number) websockets.get(0).get("pingInterval");
        return new WebsocketInfo(token, endpoint, pingInterval.longValue());
    }

    public static class WebsocketInfo {
        private final String token;
        private final String endpoint;
        private final long pingInterval;

        public WebsocketInfo(String token, String endpoint, long pingInterval) {
            this.token = token;
            this.endpoint = endpoint;
            this.pingInterval = pingInterval;
        }

        public String getToken() {
            return token;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public long getPingInterval() {
            return pingInterval;
        }

        @Override
        public String toString() {
            return "[token=" + token + ", endpoint=" + endpoint + ", pingInterval=" + pingInterval + "]";
        }
    }

    public <T> T post(String baseUrl, String path, GenericType<T> type, String body) {
        WebTarget webTarget = client.target(baseUrl).path(path);
        Invocation.Builder builder = webTarget.request("application/json");
        try (Response response = builder.post(Entity.entity(body, "application/json"))) {
            response.bufferEntity();
            if (response.getStatus() == 200) {
                try {
                    return response.readEntity(type);
                } catch (ProcessingException e) {
                    throw new RuntimeException("KucoinRestAPI> Cannot deserialize " + webTarget.toString() + " " + body
                            + ": " + response.readEntity(String.class));
                }
            } else {
                throw new RuntimeException("KucoinRestAPI> HTTP response code " + response.getStatus() + " from "
                        + webTarget.toString() + " " + body + ": " + response.readEntity(String.class));
            }
        }
    }

    public <T> T get(String baseUrl, String path, GenericType<T> type, String... query) {
        WebTarget webTarget = client.target(baseUrl).path(path);
        StringBuilder queryStr = new StringBuilder();
        if (query.length != 0) {
            for (int i = 0; i < query.length; i = i + 2) {
                String key = query[i];
                String value = query[i + 1];
                webTarget = webTarget.queryParam(key, value);
                if (i != 0) {
                    queryStr.append("&");
                }
                queryStr.append(key).append("=").append(value);
            }
        }
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON);
        try (Response response = builder.get()) {
            response.bufferEntity();
            if (response.getStatus() == 200) {
                try {
                    return response.readEntity(type);
                } catch (ProcessingException e) {
                    throw new RuntimeException("Cannot deserialize " + webTarget.toString() + " " + queryStr.toString()
                            + ": " + response.readEntity(String.class));
                }
            } else {
                throw new RuntimeException(
                        "HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
                                + webTarget.getUri().toString() + " : " + response.readEntity(String.class));
            }
        }
    }

    public static class FtxResult<T> {
        private String success;
        private T result;

        public String getSuccess() {
            return success;
        }

        public void setSuccess(String success) {
            this.success = success;
        }

        public T getResult() {
            return result;
        }

        public void setResult(T result) {
            this.result = result;
        }
    }

    public static enum Interval {
        MINUTES_1("1m", 1000 * 60), MINUTES_3("3m", MINUTES_1.millis * 3), MINUTES_5("5m", MINUTES_1.millis * 5),
        MINUTES_15("15m", MINUTES_1.millis * 15), MINUTES_30("30m", MINUTES_1.millis * 30),
        HOUR_1("1h", MINUTES_1.millis * 60), HOUR_2("2h", HOUR_1.millis * 2), HOUR_4("4h", HOUR_1.millis * 4),
        HOUR_6("6h", HOUR_1.millis * 6), HOUR_8("8h", HOUR_1.millis * 8), HOUR_12("12h", HOUR_1.millis * 12),
        DAY_1("1d", HOUR_1.millis * 24), DAY_3("3d", DAY_1.millis * 3), WEEK_1("1w", DAY_1.millis * 7),
        MONTH_1("1M", DAY_1.millis * 30);

        private final String value;
        private final long millis;

        private Interval(String value, long millis) {
            this.value = value;
            this.millis = millis;
        }

        public static Interval getInterval(long d1, long d2) {
            long difference = d2 - d1;
            Interval[] intervals = Interval.values();
            for (int i = 0; i < intervals.length; i++) {
                Interval interval = intervals[i];
                if (difference <= interval.millis) {
                    return interval;
                } else {
                    int j = i + 1;
                    if (j < intervals.length) {
                        Interval next = intervals[j];
                        if ((difference - interval.millis) < (next.millis - difference)) {
                            return interval;
                        }
                    }
                }
            }
            return MONTH_1;
        }

        public long from(long d1) {
            return d1 - (d1 % millis);
        }

        public long to(long d1) {
            return d1 + millis - 1;
        }
    }

    public static class News {
        private final String exchange;
        private final String title;
        private final Date date;
        private final String summary;
        private final String url;
        private final Set<String> delistedSymbols = new HashSet<>();

        private News(String exchange, String title, Date date, String summary, String url) {
            this.exchange = exchange;
            this.title = title;
            this.date = date;
            this.summary = summary;
            this.url = url;
        }

        @Override
        public String toString() {
            StringBuilder content = new StringBuilder()
            		.append("\n<b>").append(exchange).append("</b> ").append(Utils.fromDate(Utils.FORMAT_SECOND, date))
            		.append("\n<b>").append(title).append("</b>")
            		.append("\n").append(summary)
            		.append("\n").append(url);
            if (!delistedSymbols.isEmpty()) {
            	content.append("\nDelisted symbols:").append(delistedSymbols);
            }
            return content.toString();
        }

        public String getExchange() {
            return exchange;
        }

    }

}
