package com.jbescos.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.common.Broker.Action;

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
		List<CsvTransactionRow> transactions = fileManager.loadTransactions(cloudProperties.USER_ID);
		List<CsvRow> lastData = fileManager.loadPreviousRows(cloudProperties.USER_EXCHANGE, cloudProperties.BOT_HOURS_BACK_STATISTICS, cloudProperties.BOT_WHITE_LIST_SYMBOLS);
		Map<String, List<CsvRow>> grouped = lastData.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		Map<String, List<CsvTransactionRow>> groupedTransactions = transactions.stream()
				.collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
		Map<String, Broker> minMax = new LinkedHashMap<>();
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			List<CsvTransactionRow> symbolTransactions = groupedTransactions.get(entry.getKey());
			if (symbolTransactions != null) {
				symbolTransactions = filterLastBuys(symbolTransactions);
			}
			minMax.put(entry.getKey(), buildBroker(cloudProperties, entry.getKey(), entry.getValue(), symbolTransactions));
		}
		List<Broker> brokers = Utils.sortBrokers(minMax);
		return brokers;
	}
	
	
	private Broker buildBroker(CloudProperties cloudProperties, String symbol, List<CsvRow> rows, List<CsvTransactionRow> symbolTransactions) {
		TransactionsSummary summary = Utils.minSellProfitable(symbolTransactions);
		boolean panicPeriod = false;
		CsvRow newest = rows.get(rows.size() - 1);
		if (cloudProperties.PANIC_BROKER_ENABLE) {
			Date deadLine = Utils.getDateOfDaysBack(newest.getDate(), cloudProperties.BOT_PANIC_DAYS);
			if (Utils.isPanicSellInDays(symbolTransactions, deadLine)) {
				panicPeriod = true;
				LOGGER.warning(symbol + " is in panic period because there are panic transactions after " + Utils.fromDate(Utils.FORMAT_SECOND, deadLine));
			}
		}
		if (cloudProperties.PANIC_BROKER_ENABLE && !panicPeriod && summary.isHasTransactions() && Utils.benefit(summary.getMinProfitable(), newest.getPrice()) < cloudProperties.BOT_PANIC_RATIO) {
			return new PanicBroker(symbol, newest, summary, Action.SELL_PANIC);
		} else {
			Double fixedBuy = cloudProperties.FIXED_BUY.get(symbol);
		    if (cloudProperties.LIMITS_BROKER_ENABLE && fixedBuy != null) {
			    return new LimitsBroker(cloudProperties, symbol, rows, fixedBuy, summary);
		    } else {
				return new CautelousBroker(cloudProperties, symbol, rows, summary, panicPeriod);
			}
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

}
