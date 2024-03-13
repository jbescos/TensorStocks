package com.jbescos.exchange;

import java.util.Map;

import com.jbescos.exchange.Broker.Action;

public interface SecuredAPI {

    Map<String, String> wallet();

    CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice, boolean hasPreviousTransactions);

    CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice, boolean hasPreviousTransactions);

    CsvTransactionRow synchronize(CsvTransactionRow precalculated);
}
