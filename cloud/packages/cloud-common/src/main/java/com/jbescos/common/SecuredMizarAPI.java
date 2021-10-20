package com.jbescos.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.common.Broker.Action;

public class SecuredMizarAPI implements SecuredAPI {

	private static final String URL = "https://api.mizar.ai/api/v1";
	private static final String HEADER_API = "mizar-api-key";
    private final Client client;
    private final CloudProperties cloudProperties;
    
    private SecuredMizarAPI(Client client, CloudProperties cloudProperties) {
        this.client = client;
        this.cloudProperties = cloudProperties;
    }

    public String exchanges() {
    	String response = get("/exchanges", new GenericType<String>(){});
    	return response;
    }

    public List<String> compatibleSymbols(String exchange, String market) {
    	MizarSymbols response = get("/symbols", new GenericType<MizarSymbols>(){}, "exchange", exchange, "market", market);
    	return response.symbols.stream().map(mizarSymbol -> mizarSymbol.symbol).filter(symbol -> cloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(symbol)).collect(Collectors.toList());
    }

    public long serverTime() {
    	Map<String, Long> response = get("/server-time", new GenericType<Map<String, Long>>(){});
    	return response.get("server_time");
    }
    
    public String getOpenAllPositions() {
    	String response = get("/all-open-positions", new GenericType<String>(){}, "strategy_id", Integer.toString(cloudProperties.MIZAR_STRATEGY_ID));
    	return response;
    }
    
    public String selfHostedStrategyInfo() {
    	String response = get("/self-hosted-strategy-info", new GenericType<String>(){});
    	return response;
    }
    
    public int publishSelfHostedStrategy(String name, String description, List<String> exchanges, List<String> symbols, String market) {
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("name", name);
    	obj.put("description", description);
    	obj.put("exchanges", exchanges);
    	obj.put("symbols", symbols);
    	obj.put("market", market);
    	Map<String, Object> response = post("/publish-self-hosted-strategy", obj, new GenericType<Map<String, Object>>(){});
    	return (int) response.get("strategy_id");
    }
    
    @Override
    public Account account() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> wallet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, Double currentUsdtPrice) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, Double currentUsdtPrice) {
        // TODO Auto-generated method stub
        return null;
    }

    private <T> T get(String path, GenericType<T> type, String... query) {
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
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header(HEADER_API, cloudProperties.MIZAR_API_KEY);
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
    
	private <I, O> O post(String path, I obj, GenericType<O> responseType) {
		WebTarget webTarget = client.target(URL).path(path);;
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header(HEADER_API, cloudProperties.MIZAR_API_KEY);
		try (Response response = builder.post(Entity.json(obj))) {
			if (response.getStatus() == 200) {
				return response.readEntity(responseType);
			} else {
				response.bufferEntity();
				throw new RuntimeException("HTTP response code " + response.getStatus() + " with " + obj + " from "
						+ webTarget.getUri().toString() + " : " + response.readEntity(String.class));
			}
		}
	}

    public static class MizarSymbols {
    	public List<MizarSymbol> symbols = Collections.emptyList();
		@Override
		public String toString() {
			return "Symbols [symbols=" + symbols + "]";
		}
    }

    public static class MizarSymbol {
    	public String symbol;
    	public String base_asset;
    	public String quote_asset;
    	public String market;
		@Override
		public String toString() {
			return "MizarSymbol [symbol=" + symbol + ", base_asset=" + base_asset + ", quote_asset=" + quote_asset
					+ ", market=" + market + "]";
		}
    }

    public static SecuredMizarAPI create(CloudProperties cloudProperties, Client client) {
        return new SecuredMizarAPI(client, cloudProperties);
    }
}
