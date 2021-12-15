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
	private boolean panicPeriod;
	
	public DefaultBrokerManager(CloudProperties cloudProperties, FileManager fileManager) {
		this.cloudProperties = cloudProperties;
		this.fileManager = fileManager;
	}

	@Override
	public List<Broker> loadBrokers() throws IOException {
		List<CsvTransactionRow> transactions = fileManager.loadTransactions();
		List<CsvRow> lastData = fileManager.loadPreviousRows();
		Map<String, List<CsvRow>> grouped = lastData.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		Map<String, List<CsvTransactionRow>> groupedTransactions = transactions.stream()
				.collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
		panicPeriod = false;
		if (cloudProperties.PANIC_BROKER_ENABLE) {
			Date deadLine = Utils.getDateOfDaysBack(lastData.get(lastData.size() - 1).getDate(), cloudProperties.BOT_PANIC_DAYS);
			for (Entry<String, List<CsvTransactionRow>> entry : groupedTransactions.entrySet()) {
				if (Utils.isPanicSellInDays(entry.getValue(), deadLine)) {
					panicPeriod = true;
					LOGGER.warning("It is in panic period till " + Utils.fromDate(Utils.FORMAT_SECOND, deadLine));
					break;
				}
			}
		}
		Map<String, Broker> minMax = new LinkedHashMap<>();
		Map<String, String> benefits = new HashMap<>();
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			List<CsvTransactionRow> symbolTransactions = groupedTransactions.get(entry.getKey());
			if (symbolTransactions != null) {
				symbolTransactions = filterLastBuys(symbolTransactions);
			}
			minMax.put(entry.getKey(), buySellInstance(cloudProperties, entry.getKey(), entry.getValue(), symbolTransactions, benefits));
		}
		LOGGER.info(() -> cloudProperties.USER_ID + ": Summary of benefits " + benefits);
		List<Broker> brokers = minMax.values().stream().sorted((e2, e1) -> Double.compare(e1.getFactor(), e2.getFactor()))
				.collect(Collectors.toList());
		if (startPanicPeriod(grouped)) {
			return new SellPanicBrokerManager(cloudProperties, fileManager).loadBrokers(brokers, Action.SELL_PANIC);
		} else {
			return brokers;
		}
	}
	
	private boolean startPanicPeriod(Map<String, List<CsvRow>> grouped) {
		if (cloudProperties.PANIC_BROKER_ENABLE) {
			String btcSymbol = "BTC" + Utils.USDT;
			List<CsvRow> btcRows = grouped.get(btcSymbol);
			if (btcRows == null) {
				LOGGER.warning("BTC is not in the white list. It is important to evaluate the panic in the market");
			} else {
				CautelousBroker btc = new CautelousBroker(cloudProperties, btcSymbol, btcRows);
				if (btc.getFactor() >= cloudProperties.BOT_PANIC_RATIO && btc.inPercentileMin()) {
					LOGGER.warning(btc.getNewest() + ": BTC is too low, selling everything and starting panic period of " + cloudProperties.BOT_PANIC_DAYS + " days");
					return true;
				}
			}
		}
		return false;
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
	    } else {
			return new CautelousBroker(cloudProperties, symbol, rows, summary, panicPeriod);
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
