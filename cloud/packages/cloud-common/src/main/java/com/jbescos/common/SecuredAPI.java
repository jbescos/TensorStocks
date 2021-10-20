package com.jbescos.common;

import java.util.Map;

import com.jbescos.common.Broker.Action;

public interface SecuredAPI {

    Account account();
    
    Map<String, String> wallet();
    
    CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, Double currentUsdtPrice);
    
    CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, Double currentUsdtPrice);
}
