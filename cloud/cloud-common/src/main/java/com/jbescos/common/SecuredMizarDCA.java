package com.jbescos.common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.PropertiesMizar;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;

public class SecuredMizarDCA implements SecuredAPI {

    private static final Logger LOGGER = Logger.getLogger(SecuredMizarDCA.class.getName());
    private static final String DEFAULT_WALLET_CONTENT = "1000000000";
    private final Map<String, String> wallet = new HashMap<>();
    private static final String URL = "https://api.mizar.com/api/v1";
    private static final String HEADER_API = "mizar-api-key";
    private final Client client;
    private final PropertiesMizar cloudProperties;

    private SecuredMizarDCA(PropertiesMizar cloudProperties, Client client) {
        this.cloudProperties = cloudProperties;
        this.client = client;
        wallet.put(Utils.USDT, DEFAULT_WALLET_CONTENT);
        for (String symbol : cloudProperties.mizarWhiteListSymbols()) {
            String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
            wallet.put(walletSymbol, DEFAULT_WALLET_CONTENT);
        }
    }

    @Override
    public Map<String, String> wallet() {
        return wallet;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice) {
        CsvTransactionRow transaction = null;
        if (action == Action.BUY) {
            double quoteOrderQtyD = Double.parseDouble(quoteOrderQty);
            transaction = buy(symbol, quoteOrderQtyD / Double.parseDouble(DEFAULT_WALLET_CONTENT), currentUsdtPrice);
        } else {
            transaction = sell(symbol, action);
        }
        // FIXME Mizar is giving wrong values, we better calculate it by ourselves
        transaction = null;
        return transaction;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice) {
        CsvTransactionRow transaction = null;
        if (action == Action.BUY) {
            double quantityD = Double.parseDouble(quantity);
            transaction = buy(symbol, quantityD / Double.parseDouble(DEFAULT_WALLET_CONTENT), currentUsdtPrice);
        } else {
            transaction = sell(symbol, action);
        }
        // FIXME Mizar is giving wrong values, we better calculate it by ourselves
        transaction = null;
        return transaction;
    }

    private CsvTransactionRow buy(String symbol, double factor, Double currentUsdtPrice) {
        if (cloudProperties.mizarLimitTransactionAmount() < 0) {
            throw new IllegalStateException(
                    "SecuredMizarAPI> For Mizar limit.transaction.amount has to be higher than 0 and must match the specified amount in the strategy");
        }
        if (cloudProperties.mizarBuyIgnoreFactorReducer()) {
            factor = 1;
        }
        String asset = symbol.replaceFirst(Utils.USDT, "");
        openPosition(asset, Utils.USDT, factor);
        return null;
    }

    // symbol comes with the symbol + USDT
    public CsvTransactionRow sell(String symbol, Action action) {
        if (cloudProperties.mizarLimitTransactionAmount() < 0) {
            throw new IllegalStateException(
                    "SecuredMizarAPI> For Mizar limit.transaction.amount has to be higher than 0 and must match the specified amount in the strategy");
        }
        closeAllBySymbol(symbol);
        return null;
    }

    @Override
    public CsvTransactionRow synchronize(CsvTransactionRow precalculated) {
        throw new IllegalStateException("Mizar does not support synchornize transactions");
    }

    public Map<String, Object> openPosition(String base_asset, String quote_asset, double size) {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("bot_id", cloudProperties.mizarStrategyId());
        obj.put("base_asset", base_asset);
        obj.put("quote_asset", quote_asset);
        obj.put("side", "long");
        obj.put("position_size", size);
        obj.put("delay", 0);
        LOGGER.info(() -> "SecuredMizarAPI> Open position: " + obj);
        Map<String, Object> response = post("/dca-bots/open-position", obj, new GenericType<Map<String, Object>>() {});
        if (response != null) {
            LOGGER.info(() -> "SecuredMizarDCA> Response open position: " + response);
        }
        return response;
    }

    public Map<String, Object> closeAllBySymbol(String symbol) {
        String baseAsset = symbol.replaceFirst(Utils.USDT, "");
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("bot_id", cloudProperties.mizarStrategyId());
        obj.put("base_asset", baseAsset);
        obj.put("quote_asset", Utils.USDT);
        obj.put("delay", 0);
        LOGGER.info(() -> "SecuredMizarDCA> Close all positions: " + obj);
        Map<String, Object> response = post("/dca-bots/close-position", obj, new GenericType<Map<String, Object>>() {});
        if (response != null) {
            LOGGER.info(() -> "SecuredMizarDCA> Response close all positions: " + response);
        }
        return response;
    }

    private <I, O> O post(String path, I obj, GenericType<O> responseType) {
        WebTarget webTarget = client.target(URL).path(path);
        ;
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header(HEADER_API,
                cloudProperties.mizarApiKey());
        try (Response response = builder.post(Entity.json(obj))) {
            response.bufferEntity();
            if (response.getStatus() == 200) {
                try {
                    return response.readEntity(responseType);
                } catch (ProcessingException e) {
                    throw new RuntimeException("SecuredMizarDCA> Cannot deserialize " + webTarget.toString() + " : "
                            + response.readEntity(String.class));
                }
            } else if (response.getStatus() == 201) {
                LOGGER.warning("SecuredMizarDCA> response is 201. There are changes in Mizar API. " + response.readEntity(String.class));
                return null;
            } else {
                throw new RuntimeException("SecuredMizarDCA> HTTP response code " + response.getStatus() + " with "
                        + obj + " from " + webTarget.toString() + " : " + response.readEntity(String.class));
            }
        }
    }

    public static SecuredMizarDCA create(PropertiesMizar cloudProperties, Client client) {
        return new SecuredMizarDCA(cloudProperties, client);
    }
}
