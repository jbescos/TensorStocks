package com.jbescos.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.jbescos.exchange.SecuredKucoinAPI.KucoinResponse;


public final class PublicAPI {

	private static final Logger LOGGER = Logger.getLogger(PublicAPI.class.getName());
	private static final String FEAR_GREEDY_URL = "https://api.alternative.me/fng/";
	private static final String BINANCE_URL = "https://api.binance.com";
	private static final String KUCOIN_URL = "https://api.kucoin.com";
	private static final String OKEX_URL = "https://www.okx.com/";
	private static final String FTX_URL = "https://ftx.com";
	private final Client client;
	
	public PublicAPI(Client client) {
		this.client = client;
	}
	
	public long time() {
		return get(BINANCE_URL, "/api/v3/time", new GenericType<ServerTime>() {}).getServerTime();
	}

	public Map<String, Object> exchangeInfoFilter(String symbol, String filterName) {
	    String[] query = null;
	    if (symbol != null) {
	        query = new String[] {"symbol", symbol};
	    } else {
	        query = new String[0];
	    }
	    Map<String, Object> exchangeInfo = get(BINANCE_URL, "/api/v3/exchangeInfo", new GenericType<Map<String, Object>>() {}, query);
	    List<Map<String, Object>> symbolFilters = (List<Map<String, Object>>) exchangeInfo.get("symbols");
		return getBinanceFilter(symbol, filterName, symbolFilters);
	}
	
    private Map<String, Object> getBinanceFilter(String symbol, String filterName, List<Map<String, Object>> symbolFilters){
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
	
	public List<FearGreedIndex> getFearGreedIndex(String days){
		List<FearGreedIndex> list = new ArrayList<>();
		try {
			Map<String, Object> result = get(FEAR_GREEDY_URL, "", new GenericType<Map<String, Object>>() {}, "limit", days);
			List<Map<String, String>> data = (List<Map<String, String>>) result.get("data");
			for (Map<String, String> obj : data) {
				FearGreedIndex fearGreed = new FearGreedIndex(obj.get("value_classification"), Integer.parseInt(obj.get("value")), Long.parseLong(obj.get("timestamp")) * 1000);
				list.add(fearGreed);
			}
		} catch (Exception e) {
			FearGreedIndex fearGreed = new FearGreedIndex("Error", 50, new Date().getTime());
			LOGGER.log(Level.SEVERE, "Cannot obtain the FearGreedIndex. Setting default value to " + fearGreed, e);
			list.add(fearGreed);
		}
		return list;
	}
	
	public Map<String, Double> priceBinance() {
		List<Price> prices = get(BINANCE_URL, "/api/v3/ticker/price", new GenericType<List<Price>>() {});
		Map<String, Double> pricesBySymbol = new HashMap<>();
		prices.stream()
				.filter(price -> price.getSymbol().endsWith(Utils.USDT))
				.filter(price -> !price.getSymbol().endsWith("UP" + Utils.USDT))
				.filter(price -> !price.getSymbol().endsWith("DOWN" + Utils.USDT))
				.forEach(price -> pricesBySymbol.put(price.getSymbol(), price.getPrice()));
		return pricesBySymbol;
	}
	
	public Map<String, Double> priceKucoin() {
		return priceKucoin(ticker -> Double.parseDouble(ticker.getBuy()));
	}

	public Map<String, Double> priceKucoin(Function<Ticker, Double> price) {
		AllTickers allTickers = get(KUCOIN_URL, "/api/v1/market/allTickers", new GenericType<AllTickers>() {});
		Map<String, Double> pricesBySymbol = new HashMap<>();
		allTickers.getData().getTicker().stream()
				.filter(ticker -> ticker.getSymbol().endsWith(Utils.USDT))
				.filter(ticker -> !ticker.getSymbol().endsWith("3L-" + Utils.USDT))
				.filter(ticker -> !ticker.getSymbol().endsWith("3S-" + Utils.USDT))
				.forEach(ticker -> pricesBySymbol.put(ticker.getSymbol().replaceFirst("-", ""), price.apply(ticker)));
		return pricesBySymbol;
	}
	
	public Map<String, Double> priceOkex() {
		Map<String, Double> pricesBySymbol = new HashMap<>();
		List<Map<String, String>> data = (List<Map<String, String>>) get(OKEX_URL, "/api/v5/market/tickers", new GenericType<Map<String, Object>>() {}, "instType", "SPOT").get("data");
		data.stream()
		.filter(ticker -> ticker.get("instId").endsWith(Utils.USDT))
		.forEach(ticker -> pricesBySymbol.put(ticker.get("instId").replaceFirst("-", ""), Double.parseDouble(ticker.get("last"))));
		return pricesBySymbol;
	}

	public Map<String, Double> priceFtx() {
		Map<String, Double> pricesBySymbol = new HashMap<>();
		get(FTX_URL, "/api/markets", new GenericType<FtxResult<List<Map<String, String>>>>() {}).getResult().stream()
		.filter(ticker -> ticker.get("type").equals("spot"))
		.filter(ticker -> ticker.get("quoteCurrency").equals(Utils.USDT))
		.filter(ticker -> ticker.get("name") != null)
		.filter(ticker -> !ticker.get("name").endsWith("BULL/" + Utils.USDT))
		.filter(ticker -> !ticker.get("name").endsWith("BEAR/" + Utils.USDT))
		.forEach(ticker -> pricesBySymbol.put(ticker.get("name").replaceFirst("/", ""), Double.parseDouble(ticker.get("price"))));
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
		List<Object[]> result = get(BINANCE_URL, "/api/v3/klines", new GenericType<List<Object[]>>() {}, queryParams.toArray(new String[0]));
		List<Kline> klines = new ArrayList<>(result.size());
		for (Object[] values : result) {
			klines.add(Kline.fromArray(symbol, values));
		}
		return klines;
	}
	
	public boolean isSymbolBinanceEnabled(String symbol) {
		Map<String, Object> exchangeInfo = get(BINANCE_URL, "/api/v3/exchangeInfo", new GenericType<Map<String, Object>>() {}, "symbol", symbol);
	    Map<String, Object> symbolData = ((List<Map<String, Object>>) exchangeInfo.get("symbols")).get(0);
	    return (boolean) symbolData.get("isSpotTradingAllowed");
	}
	
	public boolean isSymbolKucoinEnabled(String symbol) {
		KucoinResponse<Map<String, Object>> result = get(KUCOIN_URL, "/api/v1/currencies/" + symbol.replaceFirst(Utils.USDT, ""), new GenericType<KucoinResponse<Map<String, Object>>>() {});
		boolean withdraw = (boolean) result.getData().get("isWithdrawEnabled");
		boolean margin = (boolean) result.getData().get("isDepositEnabled");
		return withdraw && margin;
	}
	
	public boolean isSymbolEnabled(String symbol) {
		return isSymbolBinanceEnabled(symbol) && isSymbolKucoinEnabled(symbol);
	}

	@SuppressWarnings("unchecked")
	public WebsocketInfo websocketInfo() {
		Map<String, Object> response = post(KUCOIN_URL, "/api/v1/bullet-public", new GenericType<Map<String, Object>>() {}, "");
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
                    throw new RuntimeException("KucoinRestAPI> Cannot deserialize " + webTarget.toString() + " " + body + ": " + response.readEntity(String.class));
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
                    throw new RuntimeException("Cannot deserialize " + webTarget.toString() + " " + queryStr.toString() + ": " + response.readEntity(String.class));
            	}
            } else {
                throw new RuntimeException("HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
                        + webTarget.getUri().toString() + " : " + response.readEntity(String.class));
            }
        }
    }
    
    public static class FtxResult<T>{
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
    	MINUTES_1("1m", 1000 * 60),
    	MINUTES_3("3m", MINUTES_1.millis * 3),
    	MINUTES_5("5m", MINUTES_1.millis * 5),
    	MINUTES_15("15m", MINUTES_1.millis * 15),
    	MINUTES_30("30m", MINUTES_1.millis * 30),
    	HOUR_1("1h", MINUTES_1.millis * 60),
    	HOUR_2("2h", HOUR_1.millis * 2),
    	HOUR_4("4h", HOUR_1.millis * 4),
    	HOUR_6("6h", HOUR_1.millis * 6),
    	HOUR_8("8h", HOUR_1.millis * 8),
    	HOUR_12("12h", HOUR_1.millis * 12),
    	DAY_1("1d", HOUR_1.millis * 24),
    	DAY_3("3d", DAY_1.millis * 3),
    	WEEK_1("1w", DAY_1.millis * 7),
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

}
