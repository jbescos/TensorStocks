package com.jbescos.common;

import java.util.Map;

import javax.ws.rs.client.Client;

import com.jbescos.common.Broker.Action;

public class SecuredMizarAPI implements SecuredAPI {

    private final Client client;
    private final String apiKey;
    
    private SecuredMizarAPI(Client client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
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

    public static SecuredMizarAPI create(CloudProperties cloudProperties, Client client) {
        return new SecuredMizarAPI(client, cloudProperties.MIZAR_API_KEY);
    }
}
