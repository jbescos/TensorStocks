package com.jbescos.common;

import java.util.Map;

import javax.ws.rs.client.Client;

import com.jbescos.common.Broker.Action;

public interface SecuredAPI {
    
    Map<String, String> wallet();
    
    CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice);
    
    CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice);
    
    Client getClient();
}
