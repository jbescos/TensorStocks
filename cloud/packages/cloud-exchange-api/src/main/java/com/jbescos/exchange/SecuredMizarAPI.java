package com.jbescos.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Broker.Action;

public class SecuredMizarAPI implements SecuredAPI {

	private static final Logger LOGGER = Logger.getLogger(SecuredMizarAPI.class.getName());
	private static final String DEFAULT_WALLET_CONTENT = "1000000000";
	private final Map<String, String> wallet = new HashMap<>();
	private static final String URL = "https://api.mizar.ai/api/v1";
	private static final String HEADER_API = "mizar-api-key";
    private final Client client;
    private final PropertiesMizar cloudProperties;

    private SecuredMizarAPI(PropertiesMizar cloudProperties, Client client) {
    	this.cloudProperties = cloudProperties;
        this.client = client;
        wallet.put(Utils.USDT, DEFAULT_WALLET_CONTENT);
    	for (String symbol : cloudProperties.mizarWhiteListSymbols()) {
    		String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
    		wallet.put(walletSymbol, DEFAULT_WALLET_CONTENT);
    	}
    }

    public List<String> exchanges() {
    	List<String> exchangesList = new ArrayList<>();
    	Map<String, List<Map<String, Object>>> response = get("/exchanges", new GenericType<Map<String, List<Map<String, Object>>>>(){});
    	List<Map<String, Object>> exchanges = response.get("exchanges");
    	for (Map<String, Object> exchange : exchanges) {
    		String value = (String) exchange.get("name");
    		exchangesList.add(value);
    	}
    	return exchangesList;
    }

    public List<String> compatibleSymbols(String exchange, String market) {
    	MizarSymbols response = get("/symbols", new GenericType<MizarSymbols>(){}, "exchange", exchange, "market", market);
    	Stream<String> stream = response.symbols.stream()
    			.filter(mizarSymbol -> mizarSymbol.symbol.endsWith(Utils.USDT))
    			.filter(mizarSymbol -> !mizarSymbol.symbol.endsWith("3L" + Utils.USDT))
    			.filter(mizarSymbol -> !mizarSymbol.symbol.endsWith("3S" + Utils.USDT))
    			.filter(mizarSymbol -> !mizarSymbol.symbol.endsWith("UP" + Utils.USDT))
				.filter(mizarSymbol -> !mizarSymbol.symbol.endsWith("DOWN" + Utils.USDT))
    			.map(mizarSymbol -> mizarSymbol.symbol);
    	if (!cloudProperties.mizarWhiteListSymbols().isEmpty()) {
    	    stream = stream.filter(symbol -> cloudProperties.mizarWhiteListSymbols().contains(symbol));
    	}
    	return stream.collect(Collectors.toList());   
    }

    public long serverTime() {
    	Map<String, Long> response = get("/server-time", new GenericType<Map<String, Long>>(){});
    	return response.get("server_time");
    }
    
    public OpenPositions getOpenAllPositions() {
    	OpenPositions response = get("/all-open-positions", new GenericType<OpenPositions>(){}, "strategy_id", Integer.toString(cloudProperties.mizarStrategyId()));
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
   
    public void openPosition(String base_asset, String quote_asset, double size) {
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("strategy_id", cloudProperties.mizarStrategyId());
    	obj.put("base_asset", base_asset);
    	obj.put("quote_asset", quote_asset);
    	obj.put("is_long", true);
    	obj.put("size", size);
    	LOGGER.info(() -> "SecuredMizarAPI> Open position: " + obj);
    	String response = post("/open-position", obj, new GenericType<String>(){});
    	LOGGER.info(() -> "SecuredMizarAPI> Response open position: " + response);
    }

    public void closePosition(int position_id) {
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("position_id", position_id);
    	LOGGER.info(() -> "SecuredMizarAPI> Close position: " + obj);
    	String response = post("/close-position", obj, new GenericType<String>(){});
    	LOGGER.info(() -> "SecuredMizarAPI> Response close position: " + response);
    }

    @Override
    public Map<String, String> wallet() {
        return wallet;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice) {
    	if (action == Action.BUY) {
    		double quoteOrderQtyD = Double.parseDouble(quoteOrderQty);
    		buy(symbol, quoteOrderQtyD / Double.parseDouble(DEFAULT_WALLET_CONTENT), currentUsdtPrice);
        } else {
        	sell(symbol, action);
        }
        return null;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice) {
    	if (action == Action.BUY) {
    		double quantityD = Double.parseDouble(quantity);
    		buy(symbol, quantityD / Double.parseDouble(DEFAULT_WALLET_CONTENT), currentUsdtPrice);
        } else {
        	sell(symbol, action);
        }
        return null;
    }
    
    private void buy(String symbol, double factor, Double currentUsdtPrice) {
    	if (cloudProperties.mizarLimitTransactionAmount() < 0) {
    		throw new IllegalStateException("SecuredMizarAPI> For Mizar limit.transaction.amount has to be higher than 0 and must match the specified amount in the strategy");
    	}
    	if (cloudProperties.mizarBuyIgnoreFactorReducer()) {
    		factor = 1;
    	}
    	String asset = symbol.replaceFirst(Utils.USDT, "");
    	openPosition(asset, Utils.USDT, factor);
    }

    // symbol comes with the symbol + USDT
    public void sell(String symbol, Action action) {
    	if (cloudProperties.mizarLimitTransactionAmount() < 0) {
    		throw new IllegalStateException("SecuredMizarAPI> For Mizar limit.transaction.amount has to be higher than 0 and must match the specified amount in the strategy");
    	}
    	closeAllBySymbol(symbol);
    }
    
    public void closeAllBySymbol(String symbol) {
    	String baseAsset = symbol.replaceFirst(Utils.USDT, "");
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("strategy_id", cloudProperties.mizarStrategyId());
    	obj.put("base_asset", baseAsset);
    	obj.put("quote_asset", Utils.USDT);
    	LOGGER.info(() -> "SecuredMizarAPI> Close all positions: " + obj);
    	String response = post("/close-all-positions", obj, new GenericType<String>(){});
    	LOGGER.info(() -> "SecuredMizarAPI> Response close all positions: " + response);
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
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header(HEADER_API, cloudProperties.mizarApiKey());
        try (Response response = builder.get()) {
        	response.bufferEntity();
            if (response.getStatus() == 200) {
            	try {
            		return response.readEntity(type);
            	} catch (ProcessingException e) {
                    throw new RuntimeException("SecuredMizarAPI> Cannot deserialize " + webTarget.toString() + " : " + response.readEntity(String.class));
            	}
            } else {
                throw new RuntimeException("SecuredMizarAPI> HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
                        + webTarget.toString() + " : " + response.readEntity(String.class));
            }
        }
    }
    
	private <I, O> O post(String path, I obj, GenericType<O> responseType) {
		WebTarget webTarget = client.target(URL).path(path);;
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header(HEADER_API, cloudProperties.mizarApiKey());
		try (Response response = builder.post(Entity.json(obj))) {
			response.bufferEntity();
			if (response.getStatus() == 200 || response.getStatus() == 201) {
				try {
            		return response.readEntity(responseType);
            	} catch (ProcessingException e) {
                    throw new RuntimeException("SecuredMizarAPI> Cannot deserialize " + webTarget.toString() + " : " + response.readEntity(String.class));
            	}
			} else {
				throw new RuntimeException("SecuredMizarAPI> HTTP response code " + response.getStatus() + " with " + obj + " from "
						+ webTarget.toString() + " : " + response.readEntity(String.class));
			}
		}
	}

	public static class OpenPositionResponse {
		// {"position_id":"452","strategy_id":"113","open_timestamp":1634913294278,"open_price":"10.843000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":false}
		public int position_id;
		public int strategy_id;
		public long open_timestamp;
		public String open_price;
		public String base_asset;
		public String quote_asset;
		public double size;
		public boolean is_long;
		@Override
		public String toString() {
			return "OpenPositionResponse [position_id=" + position_id + ", strategy_id=" + strategy_id
					+ ", open_timestamp=" + open_timestamp + ", open_price=" + open_price + ", base_asset=" + base_asset
					+ ", quote_asset=" + quote_asset + ", size=" + size + ", is_long=" + is_long + "]";
		}
	}
	
	public static class OpenPositions {
		public List<OpenPositionResponse> open_positions = Collections.emptyList();
		@Override
		public String toString() {
			return "OpenPositions [open_positions=" + open_positions + "]";
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

    public static SecuredMizarAPI create(PropertiesMizar cloudProperties, Client client) {
        return new SecuredMizarAPI(cloudProperties, client);
    }
    
	@Override
	public Client getClient() {
		return client;
	}

	@Override
	public CsvTransactionRow synchronize(CsvTransactionRow precalculated) {
		throw new IllegalStateException("Binance does not support synchornize transactions");
	}
}
