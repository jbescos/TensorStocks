package com.jbescos.common;

import java.util.Collections;
import java.util.Date;
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

import com.jbescos.common.Broker.Action;

public class SecuredMizarAPI implements SecuredAPI {

	private static final Logger LOGGER = Logger.getLogger(SecuredMizarAPI.class.getName());
	private static final String DEFAULT_WALLET_CONTENT = "10000000";
	private final Map<String, String> wallet = new HashMap<>();
	private static final String URL = "https://api.mizar.ai/api/v1";
	private static final String HEADER_API = "mizar-api-key";
    private final Client client;
    private final CloudProperties cloudProperties;

    private SecuredMizarAPI(CloudProperties cloudProperties, Client client) {
    	this.cloudProperties = cloudProperties;
        this.client = client;
        wallet.put(Utils.USDT, DEFAULT_WALLET_CONTENT);
    	for (String symbol : cloudProperties.BOT_WHITE_LIST_SYMBOLS) {
    		String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
    		wallet.put(walletSymbol, DEFAULT_WALLET_CONTENT);
    	}
    }

    public String exchanges() {
    	String response = get("/exchanges", new GenericType<String>(){});
    	return response;
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
    	if (!cloudProperties.BOT_WHITE_LIST_SYMBOLS.isEmpty()) {
    	    stream = stream.filter(symbol -> cloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(symbol));
    	}
    	return stream.collect(Collectors.toList());   
    }

    public long serverTime() {
    	Map<String, Long> response = get("/server-time", new GenericType<Map<String, Long>>(){});
    	return response.get("server_time");
    }
    
    public OpenPositions getOpenAllPositions() {
    	OpenPositions response = get("/all-open-positions", new GenericType<OpenPositions>(){}, "strategy_id", Integer.toString(cloudProperties.MIZAR_STRATEGY_ID));
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
   
    public OpenPositionResponse openPosition(String base_asset, String quote_asset, double size) {
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("strategy_id", cloudProperties.MIZAR_STRATEGY_ID);
    	obj.put("base_asset", base_asset);
    	obj.put("quote_asset", quote_asset);
    	obj.put("is_long", true);
    	obj.put("size", size);
    	LOGGER.info(() -> "Open position: " + obj);
    	OpenPositionResponse response = post("/open-position", obj, new GenericType<OpenPositionResponse>(){});
    	LOGGER.info(() -> "Response open position: " + response);
    	return response;
    }

    public ClosePositionResponse closePosition(int position_id) {
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("position_id", position_id);
    	LOGGER.info(() -> "Close position: " + obj);
    	ClosePositionResponse response = post("/close-position", obj, new GenericType<ClosePositionResponse>(){});
    	LOGGER.info(() -> "Response close position: " + response);
    	return response;
    }

    @Override
    public Map<String, String> wallet() {
        return wallet;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, Double currentUsdtPrice) {
    	CsvTransactionRow transaction = null;
    	if (action == Action.BUY) {
    		double quoteOrderQtyD = Double.parseDouble(quoteOrderQty);
    		transaction = buy(symbol, quoteOrderQtyD / Double.parseDouble(DEFAULT_WALLET_CONTENT), currentUsdtPrice);
        } else {
        	transaction = sell(symbol, action);
        }
        return transaction;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, Double currentUsdtPrice) {
    	CsvTransactionRow transaction = null;
    	if (action == Action.BUY) {
    		double quantityD = Double.parseDouble(quantity);
    		transaction = buy(symbol, quantityD / Double.parseDouble(DEFAULT_WALLET_CONTENT), currentUsdtPrice);
        } else {
        	transaction = sell(symbol, action);
        }
        return transaction;
    }
    
    private CsvTransactionRow buy(String symbol, double factor, Double currentUsdtPrice) {
    	if (cloudProperties.LIMIT_TRANSACTION_AMOUNT < 0) {
    		throw new IllegalStateException("For Mizar limit.transaction.amount has to be higher than 0 and must match the specified amount in the strategy");
    	}
    	if (cloudProperties.BOT_BUY_IGNORE_FACTOR_REDUCER) {
    		factor = 1;
    	}
    	String asset = symbol.replaceFirst(Utils.USDT, "");
    	double usdtToBuy = cloudProperties.LIMIT_TRANSACTION_AMOUNT * factor;
    	OpenPositionResponse open = openPosition(asset, Utils.USDT, factor);
    	Double currentPrice = Double.parseDouble(open.open_price);
    	if (currentUsdtPrice != null && (currentPrice.isNaN() || currentPrice.isInfinite() || currentPrice == 0)) {
    		LOGGER.warning("Current open price from Mizar is not a valid number in " + open + ". Taking our value of " + Utils.format(currentUsdtPrice));
    		currentPrice = currentUsdtPrice;
    	}
    	double quantity = Utils.symbolValue(usdtToBuy, currentPrice);
    	CsvTransactionRow transaction = new CsvTransactionRow(new Date(open.open_timestamp), Integer.toString(open.position_id), Action.BUY, symbol, Utils.format(usdtToBuy), Utils.format(quantity), Double.parseDouble(open.open_price));
    	return transaction;
    }

    // symbol comes with the symbol + USDT
    public CsvTransactionRow sell(String symbol, Action action) {
    	if (cloudProperties.LIMIT_TRANSACTION_AMOUNT < 0) {
    		throw new IllegalStateException("For Mizar limit.transaction.amount has to be higher than 0 and must match the specified amount in the strategy");
    	}
    	ClosePositionsResponse response = closeAllBySymbol(symbol);
    	if (response.closed_positions == null || response.closed_positions.isEmpty()) {
    		LOGGER.severe("It was requested to sell " + symbol + ". But there are no open positions for that. There is a missmatch between the data we have and Mizar. " + response + ". Returning a fake transaction to bypass this.");
    		CsvTransactionRow transaction = new CsvTransactionRow(new Date(), "ERROR", action, symbol, "0.000001", "0.000001", 0.000001);
    		return transaction;
    	} else {
	    	StringBuilder orderIds = new StringBuilder();
	    	double totalQuantity = Utils.totalQuantity(cloudProperties.LIMIT_TRANSACTION_AMOUNT, response.closed_positions);
	    	for (ClosePositionResponse position : response.closed_positions) {
	    		if (orderIds.length() != 0) {
	    			orderIds.append("-");
	    		}
	    		orderIds.append(position.position_id);
	    	}
	    	ClosePositionResponse last = response.closed_positions.get(response.closed_positions.size() - 1);
	    	double totalUsdt = Double.parseDouble(last.close_price) * totalQuantity;
	    	CsvTransactionRow transaction = new CsvTransactionRow(new Date(last.close_timestamp), orderIds.toString(), action, symbol, Utils.format(totalUsdt), Utils.format(totalQuantity), Double.parseDouble(last.close_price));
	    	return transaction;
    	}
    }
    
    public ClosePositionsResponse closeAllBySymbol(String symbol) {
    	String baseAsset = symbol.replaceFirst(Utils.USDT, "");
    	Map<String, Object> obj = new LinkedHashMap<>();
    	obj.put("strategy_id", cloudProperties.MIZAR_STRATEGY_ID);
    	obj.put("base_asset", baseAsset);
    	obj.put("quote_asset", Utils.USDT);
    	LOGGER.info(() -> "Close all positions: " + obj);
    	ClosePositionsResponse response = post("/close-all-positions", obj, new GenericType<ClosePositionsResponse>(){});
    	LOGGER.info(() -> "Response close all positions: " + response);
    	return response;
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
        	response.bufferEntity();
            if (response.getStatus() == 200) {
            	try {
            		return response.readEntity(type);
            	} catch (ProcessingException e) {
                    throw new RuntimeException("Cannot deserialize " + webTarget.toString() + " : " + response.readEntity(String.class));
            	}
            } else {
                throw new RuntimeException("HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
                        + webTarget.toString() + " : " + response.readEntity(String.class));
            }
        }
    }
    
	private <I, O> O post(String path, I obj, GenericType<O> responseType) {
		WebTarget webTarget = client.target(URL).path(path);;
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header(HEADER_API, cloudProperties.MIZAR_API_KEY);
		try (Response response = builder.post(Entity.json(obj))) {
			response.bufferEntity();
			if (response.getStatus() == 200) {
				try {
            		return response.readEntity(responseType);
            	} catch (ProcessingException e) {
                    throw new RuntimeException("Cannot deserialize " + webTarget.toString() + " : " + response.readEntity(String.class));
            	}
			} else {
				throw new RuntimeException("HTTP response code " + response.getStatus() + " with " + obj + " from "
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
	
	public static class ClosePositionsResponse {
		// {"closed_positions":[{"position_id":"769","strategy_id":"113","open_timestamp":1635404904035,"close_timestamp":1635405060644,"open_price":"10.822000000000","close_price":"10.822000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":true},{"position_id":"770","strategy_id":"113","open_timestamp":1635404911672,"close_timestamp":1635405060663,"open_price":"10.822000000000","close_price":"10.822000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":true}]}
    	public List<ClosePositionResponse> closed_positions = Collections.emptyList();

		@Override
		public String toString() {
			return "ClosePositionsResponse [closed_positions=" + closed_positions + "]";
		}
	}
	
	public static class ClosePositionResponse {
		// {"position_id":"453","strategy_id":"113","open_timestamp":1634913995389,"close_timestamp":1634914090671,"open_price":"10.827000000000","close_price":"10.843000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":false}
		public int position_id;
		public int strategy_id;
		public long open_timestamp;
		public long close_timestamp;
		public String open_price;
		public String close_price;
		public String base_asset;
		public String quote_asset;
		public double size;
		public boolean is_long;
		@Override
		public String toString() {
			return "ClosePositionResponse [position_id=" + position_id + ", strategy_id=" + strategy_id
					+ ", open_timestamp=" + open_timestamp + ", close_timestamp=" + close_timestamp + ", open_price="
					+ open_price + ", close_price=" + close_price + ", base_asset=" + base_asset + ", quote_asset="
					+ quote_asset + ", size=" + size + ", is_long=" + is_long + "]";
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

    public static SecuredMizarAPI create(CloudProperties cloudProperties, Client client) {
        return new SecuredMizarAPI(cloudProperties, client);
    }
}
