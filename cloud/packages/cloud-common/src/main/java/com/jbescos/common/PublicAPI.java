package com.jbescos.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.common.kucoin.AllTickers;

public final class PublicAPI {

	private static final String BINANCE_URL = "https://api.binance.com";
	private static final String KUCOIN_URL = "https://api.kucoin.com";
	private static final String OKEX_URL = "https://www.okex.com";
	private static final String FTX_URL = "https://ftx.com";
	private final Client client;
	
	public PublicAPI(Client client) {
		this.client = client;
	}
	
	public long time() {
		return get(BINANCE_URL, "/api/v3/time", new GenericType<ServerTime>() {}).getServerTime();
	}
	
	public ExchangeInfo exchangeInfo(String symbol) {
	    String[] query = null;
	    if (symbol != null) {
	        query = new String[] {"symbol", symbol};
	    } else {
	        query = new String[0];
	    }
		ExchangeInfo exchangeInfo = get(BINANCE_URL, "/api/v3/exchangeInfo", new GenericType<ExchangeInfo>() {}, query);
		return exchangeInfo;
	}
	
	public List<Price> priceBinance() {
		List<Price> prices = get(BINANCE_URL, "/api/v3/ticker/price", new GenericType<List<Price>>() {});
		prices = prices.stream()
				.filter(price -> price.getSymbol().endsWith(Utils.USDT))
				.filter(price -> !price.getSymbol().endsWith("UP" + Utils.USDT))
				.filter(price -> !price.getSymbol().endsWith("DOWN" + Utils.USDT))
				.collect(Collectors.toList());
		return prices;
	}
	
	public List<Price> priceKucoin() {
		AllTickers allTickers = get(KUCOIN_URL, "/api/v1/market/allTickers", new GenericType<AllTickers>() {});
		List<Price> prices = allTickers.getData().getTicker().stream()
				.filter(ticker -> ticker.getSymbol().endsWith(Utils.USDT))
				.filter(ticker -> !ticker.getSymbol().endsWith("3L" + Utils.USDT))
				.filter(ticker -> !ticker.getSymbol().endsWith("3S" + Utils.USDT))
				.map(ticker -> new Price(ticker.getSymbol().replaceFirst("-", ""), Double.parseDouble(ticker.getBuy())))
				.collect(Collectors.toList());
		return prices;
	}
	
	public List<Price> priceOkex() {
		return get(OKEX_URL, "/api/spot/v3/instruments/ticker", new GenericType<List<Map<String, String>>>() {}).stream()
		.filter(ticker -> ticker.get("instrument_id").endsWith(Utils.USDT))
		.map(ticker -> new Price(ticker.get("instrument_id").replaceFirst("-", ""), Double.parseDouble(ticker.get("last"))))
		.collect(Collectors.toList());
	}

	public List<Price> priceFtx() {
		return get(FTX_URL, "/api/markets", new GenericType<FtxResult<List<Map<String, String>>>>() {}).getResult().stream()
		.filter(ticker -> ticker.get("type").equals("spot"))
		.filter(ticker -> ticker.get("quoteCurrency").equals(Utils.USDT))
		.map(ticker -> new Price(ticker.get("name").replaceFirst("/", ""), Double.parseDouble(ticker.get("price"))))
		.filter(price -> !price.getSymbol().endsWith("BULL" + Utils.USDT))
		.filter(price -> !price.getSymbol().endsWith("BEAR" + Utils.USDT))
		.collect(Collectors.toList());
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
			klines.add(Kline.fromArray(values));
		}
		return klines;
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
