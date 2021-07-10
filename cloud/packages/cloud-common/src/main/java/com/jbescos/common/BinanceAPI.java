package com.jbescos.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public final class BinanceAPI {

	private static final String URL = "https://api.binance.com";
	
	public static ExchangeInfo exchangeInfo(String symbol) {
	    String[] query = null;
	    if (symbol != null) {
	        query = new String[] {"symbol", symbol};
	    } else {
	        query = new String[0];
	    }
		ExchangeInfo exchangeInfo = BinanceAPI.get("/api/v3/exchangeInfo", new GenericType<ExchangeInfo>() {}, query);
		return exchangeInfo;
	}
	
	public static List<Price> price() {
		List<Price> prices = BinanceAPI.get("/api/v3/ticker/price", new GenericType<List<Price>>() {});
		prices = prices.stream().filter(price -> price.getSymbol().endsWith("USDT"))
				.filter(price -> !price.getSymbol().endsWith("UPUSDT"))
				.filter(price -> !price.getSymbol().endsWith("DOWNUSDT")).collect(Collectors.toList());
		return prices;
	}
	
	public static List<Kline> klines(Interval interval, String symbol, int limit, long startTime, long endTime) {
		List<Object[]> result = BinanceAPI.get("/api/v3/klines", new GenericType<List<Object[]>>() {},
				new String[] {"interval", interval.value, "symbol", symbol, "limit", Integer.toString(limit), "startTime", Long.toString(startTime), "endTime", Long.toString(endTime)});
		List<Kline> klines = new ArrayList<>(result.size());
		for (Object[] values : result) {
			klines.add(Kline.fromArray(values));
		}
		return klines;
	}

    public static <T> T get(String path, GenericType<T> type, String... query) {
        Client client = ClientBuilder.newClient();
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
    	MINUTES_30("30m");
    	
    	private final String value;
    	
    	private Interval(String value) {
    		this.value = value;
    	}
    }

}
