package com.jbescos.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;

public class SecuredArkenAPI implements SecuredAPI {

    private static final String URL_DEV = "https://devpublic-api.arken.finance";
    private static final String URL = "https://public-api.arken.finance";
    private static final String USDT_ID = "tether";
    private static final String SLIPPAGE = "5.00";
    private static final Map<String, Integer> CHAIN_IDS = new HashMap<>();
//    private static final String URL = "https://devpublic-api.arken.finance";
    private final Client client;
    private final String userName;
    private final String apiToken;
    private final String chain;
    private final String poolAddress;
    private final int chainId;
    private final String usdtToken;
    private final Map<String, String> symbolsTokens = new HashMap<>();
    private final Map<String, String> tokensSymbols = new HashMap<>();
    
    static {
        // 1 = Ethereum Mainnet 56 = BNB Smart Chain 97 = BSC Testnet 42161 = Arbitrum
        // "polygon","ethereum","avalanche","aurora","arbitrum","bsc","rei"
        CHAIN_IDS.put("ethereum", 1);
//        CHAIN_IDS.put("bsc", 56); // BSC real
        CHAIN_IDS.put("bsc", 97); // BSC test
        CHAIN_IDS.put("arbitrum", 42161);
    }

    public SecuredArkenAPI(Client client, String userName, String apiToken, String chain, String poolAddress) {
        this.client = client;
        this.userName = userName;
        this.apiToken = apiToken;
        this.chain = chain;
        this.poolAddress = poolAddress;
        this.chainId = CHAIN_IDS.get(chain);
        Properties tokens = new Properties();
        String properties = "/" + chain + ".properties";
        try (InputStream in = SecuredArkenAPI.class.getResourceAsStream(properties)) {
            tokens.load(in);
            for (Entry<Object, Object> entry : tokens.entrySet()) {
                symbolsTokens.put((String) entry.getKey(), (String) entry.getValue());
                tokensSymbols.put((String) entry.getValue(), (String) entry.getKey());
            }
            usdtToken = symbolsTokens.get(USDT_ID);
        } catch (IOException e) {
            throw new IllegalStateException("Missing " + properties, e);
        }
    }

    public Map<String, Price> price() {
        // {"chains":["polygon","ethereum","avalanche","aurora","arbitrum","bsc","rei"]}
        // Map<String, String> chains = get(ARKEN_URL, "/insider/v1/chains", new GenericType<Map<String, String>>() {});
        Map<String, Price> data = new HashMap<>();
        List<String> tokens = new ArrayList<>(symbolsTokens.values());
        // Avoid the URL is too big to fail
        final int LIST_LIMIT = 70;
        int i = 0;
        do {
            int to = i + LIST_LIMIT;
            if (to > tokens.size()) {
                to = tokens.size();
            }
            List<String> subIds = tokens.subList(i, to).stream().collect(Collectors.toList());
            String key = String.join(",", subIds);
            Map<String, Map<String, Double>> chunk = get(URL, "/insider/v1/" + chain + "/tokens/price", new GenericType<Map<String, Map<String, Double>>>() {}, "addresses", key);
            for (Entry<String, Map<String, Double>> entry : chunk.entrySet()) {
                String token = entry.getKey();
                Double price = entry.getValue().get("price");
                String symbol = tokensSymbols.get(token) + Utils.USDT;
                data.put(symbol, new Price(symbol, price, token));
            }
            i = to;
        } while (i < tokens.size());
        return data;
    }
    
    private Map<String, Object> order(String baseToken, String quoteToken, String quoteAmount, Action action) {
        Map<String, Object> request = new HashMap<>();
        request.put("chainID", chainId);
        request.put("poolAddress", poolAddress);
        request.put("baseToken", baseToken);
        request.put("quoteToken", quoteToken);
        request.put("quoteAmount", quoteAmount);
        request.put("side", action.side());
        request.put("slippage", SLIPPAGE);
        Map<String, Object> response = post(URL_DEV, "/fund-manager/order/market/pre-create", new GenericType<Map<String, Object>>() {}, request);
        System.out.println(response);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        String orderId = (String) data.get("orderID");
        return response;
    }

    public Map<String, Object> pool() {
        Map<String, Object> data = get(URL_DEV, "/fund-manager/pool/" + chainId + "/" + poolAddress, new GenericType<Map<String, Object>>() {});
        return data;
    }

    public Map<String, Object> chains() {
        Map<String, Object> chains = get(URL, "/insider/v1/chains", new GenericType<Map<String, Object>>() {});
        return chains;
    }
    
    @Override
    public Map<String, String> wallet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice) {
//        order(usdtToken, symbolsTokens.get(symbol), quoteOrderQty, action);
        // FIXME, they only support these 2 tokens
        order("0x3e9a5e2b6758c7dc1dca2d46abf1ef215a2ec6ef", "0xf0c49279bef38df8479b4f8c08fafa8f99b4794c", quoteOrderQty, action);
        return null;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice) {
        order(symbolsTokens.get(symbol), usdtToken, quantity, action);
        return null;
    }

    @Override
    public CsvTransactionRow synchronize(CsvTransactionRow precalculated) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T get(String url, String path, GenericType<T> type, String... query) {
        WebTarget webTarget = client.target(url).path(path);
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
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header("X-API-Username", userName).header("X-API-Token", apiToken);
        try (Response response = builder.get()) {
            if (response.getStatus() == 200) {
                return response.readEntity(type);
            } else {
                response.bufferEntity();
                throw new RuntimeException("SecuredArkenAPI> HTTP response code " + response.getStatus()
                        + " with query " + queryStr.toString() + " from " + webTarget.toString() + " : "
                        + response.readEntity(String.class));
            }
        }
    }
    
    public <T> T post(String url, String path, GenericType<T> type, Object body) {
        WebTarget webTarget = client.target(url).path(path);
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header("X-API-Username", userName).header("X-API-Token", apiToken);
        try (Response response = builder.post(Entity.entity(body, "application/json"))) {
            if (response.getStatus() == 200) {
                return response.readEntity(type);
            } else {
                response.bufferEntity();
                throw new RuntimeException("SecuredKucoinAPI> HTTP response code " + response.getStatus() + " from "
                        + webTarget.toString() + " " + body + ": " + response.readEntity(String.class));
            }
        }
    }
}
