package com.jbescos.exchange;

import java.util.Map;

import javax.ws.rs.client.Client;

import com.jbescos.exchange.Broker.Action;

public interface SecuredAPI {

    Map<String, String> wallet();

    CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice);

    CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice);

    Client getClient();

    CsvTransactionRow synchronize(CsvTransactionRow precalculated);
}
