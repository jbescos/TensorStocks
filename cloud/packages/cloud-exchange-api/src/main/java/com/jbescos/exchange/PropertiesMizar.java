package com.jbescos.exchange;

import java.util.List;

public interface PropertiesMizar {

    List<String> mizarWhiteListSymbols();

    int mizarStrategyId();

    String mizarApiKey();

    double mizarLimitTransactionAmount();

    boolean mizarBuyIgnoreFactorReducer();
}
