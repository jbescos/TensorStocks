package com.jbescos.common;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.TransactionsSummary;
import com.jbescos.exchange.Utils;
import com.jbescos.exchange.Broker.Action;

public class DefaultBrokerManager implements BrokerManager {

    private static final Logger LOGGER = Logger.getLogger(DefaultBrokerManager.class.getName());
    private final CloudProperties cloudProperties;
    private final FileManager fileManager;

    public DefaultBrokerManager(CloudProperties cloudProperties, FileManager fileManager) {
        this.cloudProperties = cloudProperties;
        this.fileManager = fileManager;
    }

    @Override
    public List<Broker> loadBrokers() throws IOException {
        List<CsvTransactionRow> transactions = fileManager.loadOpenTransactions(cloudProperties.USER_ID);
        List<CsvRow> lastData = fileManager.loadPreviousRows(cloudProperties.USER_EXCHANGE.getFolder(),
                cloudProperties.BOT_HOURS_BACK_STATISTICS, cloudProperties.BOT_WHITE_LIST_SYMBOLS);
        Map<String, List<CsvRow>> grouped = lastData.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
        Map<String, List<CsvTransactionRow>> groupedTransactions = transactions.stream()
                .collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
        Map<String, Broker> minMax = new LinkedHashMap<>();
        for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
            List<CsvTransactionRow> symbolTransactions = groupedTransactions.get(entry.getKey());
            minMax.put(entry.getKey(),
                    buildBroker(cloudProperties, entry.getKey(), entry.getValue(), symbolTransactions));
        }
        List<Broker> brokers = Utils.sortBrokers(minMax);
        return brokers;
    }

    private Broker buildBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> rows,
            List<CsvTransactionRow> symbolTransactions) {
        TransactionsSummary summary = Utils.minSellProfitable(symbolTransactions);
        Double fixedBuy = cloudProperties.FIXED_BUY.get(symbol);
        if (cloudProperties.LIMITS_BROKER_ENABLE && fixedBuy != null) {
            return new LimitsBroker(cloudProperties, symbol, rows, fixedBuy, summary);
        } else if (cloudProperties.BOT_DCA_RATIO_BUY > 0) {
            return new DCABroker(cloudProperties, symbol, rows, summary);
        } else {
            return new LowBroker(cloudProperties, symbol, rows, summary);
        }
    }

}
