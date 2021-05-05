package com.jbescos.common;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

public final class BinanceAPI {

	private static final String URL = "https://api.binance.com";

	public static <T> T get(String path, String query, GenericType<T> type) {
		Client client = ClientBuilder.newClient(new ClientConfig());
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
