package com.jbescos.common;

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
	
	public static ExchangeInfo exchangeInfo() {
		ExchangeInfo exchangeInfo = BinanceAPI.get("/api/v3/exchangeInfo", null, new GenericType<ExchangeInfo>() {});
		return exchangeInfo;
	}
	
	public static List<Price> price() {
		List<Price> prices = BinanceAPI.get("/api/v3/ticker/price", null, new GenericType<List<Price>>() {});
		prices = prices.stream().filter(price -> price.getSymbol().endsWith("USDT")).collect(Collectors.toList());
		return prices;
	}

	public static <T> T get(String path, String query, GenericType<T> type) {
		Client client = ClientBuilder.newClient();
		if (query != null) {
			path = path + "?" + query;
		}
		WebTarget webTarget = client.target(URL).path(path);
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON);
		try (Response response = builder.get()) {
			if (response.getStatus() == 200) {
				return response.readEntity(type);
			} else {
				response.bufferEntity();
				throw new RuntimeException("HTTP response code " + response.getStatus() + " from "
						+ webTarget.getUri().toString() + " : " + response.readEntity(String.class));
			}
		}
	}

}
