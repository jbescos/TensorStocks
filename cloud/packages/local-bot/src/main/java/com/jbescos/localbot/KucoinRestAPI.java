package com.jbescos.localbot;

import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

public class KucoinRestAPI {

	private static final String API_URL = "https://api.kucoin.com";
	private final Client client;

	public KucoinRestAPI(Client client) {
		this.client = client;
	}

	public <T> T post(String path, GenericType<T> type, String body) {
		WebTarget webTarget = client.target(API_URL).path(path);
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
	
	@SuppressWarnings("unchecked")
	public WebsocketInfo websocketInfo() {
		Map<String, Object> response = post("/api/v1/bullet-public", new GenericType<Map<String, Object>>() {}, "");
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
}