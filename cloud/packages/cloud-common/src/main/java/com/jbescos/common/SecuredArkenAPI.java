package com.jbescos.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.SecuredAPI;

public class SecuredArkenAPI implements SecuredAPI {

    private static final String URL = "https://public-api.arken.finance";
    private static final Map<String, Integer> CHAIN_IDS = new HashMap<>();
//    private static final String URL = "https://devpublic-api.arken.finance";
    private final Client client;
    private final String userName;
    private final String apiToken;
    private final String chain;
    private final String poolAddress;
    private final int chainId;
    private final Properties tokens;
    
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
        this.tokens = new Properties();
        String properties = "/" + chain + ".properties";
        try (InputStream in = SecuredArkenAPI.class.getResourceAsStream(properties)) {
            this.tokens.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Missing " + properties, e);
        }
    }

    public Map<String, Object> price(String ... tokens) {
        // {"chains":["polygon","ethereum","avalanche","aurora","arbitrum","bsc","rei"]}
        // Map<String, String> chains = get(ARKEN_URL, "/insider/v1/chains", new GenericType<Map<String, String>>() {});
        Map<String, Object> data = get("/insider/v1/" + chain + "/tokens/price", new GenericType<Map<String, Object>>() {}, tokens);
        return data;
    }

    public Map<String, Object> pool() {
        Map<String, Object> data = get("/fund-manager/pool/" + chainId + "/" + poolAddress, new GenericType<Map<String, Object>>() {});
        return data;
    }

    public Map<String, Object> chains() {
        Map<String, Object> chains = get("/insider/v1/chains", new GenericType<Map<String, Object>>() {});
        return chains;
    }
    
    @Override
    public Map<String, String> wallet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CsvTransactionRow synchronize(CsvTransactionRow precalculated) {
        // TODO Auto-generated method stub
        return null;
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
}
