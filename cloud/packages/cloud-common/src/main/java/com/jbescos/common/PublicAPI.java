package com.jbescos.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public final class PublicAPI {

	private static final String URL = "https://api.binance.com";
	private final Client client;
	
	public PublicAPI(Client client) {
		this.client = client;
	}
	
	public long time() {
		return get("/api/v3/time", new GenericType<ServerTime>() {}).getServerTime();
	}
	
	public ExchangeInfo exchangeInfo(String symbol) {
	    String[] query = null;
	    if (symbol != null) {
	        query = new String[] {"symbol", symbol};
	    } else {
	        query = new String[0];
	    }
		ExchangeInfo exchangeInfo = get("/api/v3/exchangeInfo", new GenericType<ExchangeInfo>() {}, query);
		return exchangeInfo;
	}
	
	public List<Price> price() {
		List<Price> prices = get("/api/v3/ticker/price", new GenericType<List<Price>>() {});
		prices = prices.stream().filter(price -> price.getSymbol().endsWith("USDT"))
				.filter(price -> !price.getSymbol().endsWith("UPUSDT"))
				.filter(price -> !price.getSymbol().endsWith("DOWNUSDT")).collect(Collectors.toList());
		return prices;
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
		List<Object[]> result = get("/api/v3/klines", new GenericType<List<Object[]>>() {}, queryParams.toArray(new String[0]));
		List<Kline> klines = new ArrayList<>(result.size());
		for (Object[] values : result) {
			klines.add(Kline.fromArray(values));
		}
		return klines;
	}

    public <T> T get(String path, GenericType<T> type, String... query) {
        WebTarget webTarget = client.target(URL).path(path);
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
            if (response.getStatus() == 200) {
                return response.readEntity(type);
            } else {
                response.bufferEntity();
                throw new RuntimeException("HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
                        + webTarget.getUri().toString() + " : " + response.readEntity(String.class));
            }
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