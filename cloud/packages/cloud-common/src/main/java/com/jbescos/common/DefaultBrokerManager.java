package com.jbescos.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.common.Broker.Action;
import com.jbescos.common.CloudProperties.FixedBuySell;

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
		List<CsvTransactionRow> transactions = fileManager.loadTransactions();
		List<CsvRow> lastData = fileManager.loadPreviousRows();
		Map<String, Broker> minMax = new LinkedHashMap<>();
		Map<String, List<CsvRow>> grouped = lastData.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		Map<String, List<CsvTransactionRow>> groupedTransactions = transactions.stream()
				.collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
		Date deadLine = Utils.getDateOfDaysBack(new Date(), cloudProperties.BOT_PANIC_DAYS);
		Map<String, String> benefits = new HashMap<>();
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			List<CsvTransactionRow> symbolTransactions = groupedTransactions.get(entry.getKey());
			if (!Utils.isPanicSellInDays(symbolTransactions, deadLine)) {
    			if (symbolTransactions != null) {
    				symbolTransactions = filterLastBuys(symbolTransactions);
    			}
    			minMax.put(entry.getKey(), buySellInstance(cloudProperties, entry.getKey(), entry.getValue(), symbolTransactions, benefits));
			} else {
			    LOGGER.info(() -> entry.getKey() + " skipped because there was a SELL_PANIC recently");
			}
		}
		LOGGER.info(() -> cloudProperties.USER_ID + ": Summary of benefits " + benefits);
		return minMax.values().stream().sorted((e2, e1) -> Double.compare(e1.getFactor(), e2.getFactor()))
				.collect(Collectors.toList());
	}
	
	private Broker buySellInstance(CloudProperties cloudProperties, String symbol, List<CsvRow> rows, List<CsvTransactionRow> symbolTransactions, Map<String, String> benefits) {
		TransactionsSummary summary = Utils.minSellProfitable(symbolTransactions);
		CsvRow newest = rows.get(rows.size() - 1);
		if (summary.isHasTransactions()) {
		    benefits.put(symbol, Utils.format(benefit(summary.getMinProfitable(), newest.getPrice())));
		}
		FixedBuySell fixedBuySell = cloudProperties.FIXED_BUY_SELL.get(symbol);
	    if (cloudProperties.LIMITS_BROKER_ENABLE && fixedBuySell != null) {
		    return new LimitsBroker(cloudProperties, symbol, rows, fixedBuySell, summary);
	    } else if (cloudProperties.PANIC_BROKER_ENABLE && PanicBroker.isPanic(cloudProperties, newest, summary.getMinProfitable())) {
			return new PanicBroker(symbol, newest, summary);
		} else {
			return new CautelousBroker(cloudProperties, symbol, rows, summary);
		}
	}

	private List<CsvTransactionRow> filterLastBuys(List<CsvTransactionRow> symbolTransactions) {
		List<CsvTransactionRow> filtered = new ArrayList<>();
		for (int i = symbolTransactions.size() - 1; i >= 0; i--) {
			CsvTransactionRow tx = symbolTransactions.get(i);
			if (tx.getSide() == Action.BUY) {
				filtered.add(tx);
			} else {
				break;
			}
		}
		return filtered;
	}
	
	private double benefit(double minProfitableSellPrice, double currentPrice) {
	    if (currentPrice > minProfitableSellPrice) {
	        return 1 - (minProfitableSellPrice/currentPrice);
	    } else {
	        return -1 * (1 - (currentPrice/minProfitableSellPrice));
	    }
	}

}
