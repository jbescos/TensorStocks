package com.jbescos.cloudbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.Broker;
import com.jbescos.common.Broker.Action;
import com.jbescos.common.CautelousBroker;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.FixedBuySell;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.GreedyBroker;
import com.jbescos.common.LimitsBroker;
import com.jbescos.common.PanicBroker;
import com.jbescos.common.Utils;

public class BotUtils {

	private static final Logger LOGGER = Logger.getLogger(BotUtils.class.getName());

	public static List<Broker> loadStatistics(CloudProperties cloudProperties, Client client, boolean requestLatestPrices) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService();
		// Get 1 day more and compare dates later
		List<String> days = Utils.daysBack(new Date(), (cloudProperties.BOT_HOURS_BACK_STATISTICS / 24) + 1, "data/", ".csv");
		List<CsvRow> rows = new ArrayList<>();
		Date now = new Date();
		Date from = Utils.getHoursOfDaysBack(now, cloudProperties.BOT_HOURS_BACK_STATISTICS);
		List<CsvRow> csvInDay = null;
		for (String day : days) {
			try (ReadChannel readChannel = storage.reader(cloudProperties.BUCKET, day);
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				csvInDay = CsvUtil.readCsvRows(true, ",", reader, cloudProperties.BOT_WHITE_LIST_SYMBOLS);
				csvInDay = csvInDay.stream().filter(row -> row.getDate().getTime() > from.getTime())
						.collect(Collectors.toList());
				rows.addAll(csvInDay);
				LOGGER.info("Loaded " + csvInDay.size() + " rows from " + day);
			}
		}
		if (!rows.isEmpty()) {
			LOGGER.info(() -> "Data is obtained from " + Utils.fromDate(Utils.FORMAT_SECOND, rows.get(0).getDate()) + " to "
					+ Utils.fromDate(Utils.FORMAT_SECOND, now));
		}
		List<CsvTransactionRow> transactions = new ArrayList<>();
		List<String> months = Utils.monthsBack(new Date(), cloudProperties.BOT_MONTHS_BACK_TRANSACTIONS, cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
		Page<Blob> transactionFiles = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX));
		for (Blob transactionFile : transactionFiles.iterateAll()) {
		    if (months.contains(transactionFile.getName())) {
		        try (ReadChannel readChannel = transactionFile.reader();
                        BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
                    List<CsvTransactionRow> csv = CsvUtil.readCsvTransactionRows(true, ",", reader);
                    transactions.addAll(csv);
                }
		    }
		}
		LOGGER.info(() -> "Transactions loaded: " + transactions.size() + " from " + months);
		if (requestLatestPrices) {
			List<CsvRow> latestCsv = new BinanceAPI(client).price().stream()
					.map(price -> new CsvRow(now, price.getSymbol(), price.getPrice()))
					.filter(row -> cloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(row.getSymbol()))
					.collect(Collectors.toList());
			for (CsvRow last : latestCsv) {
				for (CsvRow inDay : csvInDay) {
					if (last.getSymbol().equals(inDay.getSymbol())) {
						last.setAvg(Utils.ewma(cloudProperties.EWMA_CONSTANT, last.getPrice(), inDay.getAvg()));
						last.setAvg2(Utils.ewma(cloudProperties.EWMA_2_CONSTANT, last.getPrice(), inDay.getAvg2()));
						break;
					}
				}
			}
			rows.addAll(latestCsv);
		}
		return fromCsvRows(cloudProperties, rows, transactions);
	}

	public static List<Broker> fromCsvRows(CloudProperties cloudProperties, List<CsvRow> csv, List<CsvTransactionRow> transactions) {
		Map<String, Broker> minMax = new LinkedHashMap<>();
		Map<String, List<CsvRow>> grouped = csv.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		Map<String, List<CsvTransactionRow>> groupedTransactions = transactions.stream()
				.collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
		LOGGER.info(() -> "There is data for: " + grouped.keySet());
		Date deadLine = Utils.getDateOfDaysBack(new Date(), cloudProperties.BOT_PANIC_DAYS);
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			List<CsvTransactionRow> symbolTransactions = groupedTransactions.get(entry.getKey());
			if (!Utils.isPanicSellInDays(symbolTransactions, deadLine)) {
    			if (symbolTransactions != null) {
    				symbolTransactions = filterLastBuys(symbolTransactions);
    			}
    			minMax.put(entry.getKey(), buySellInstance(cloudProperties, entry.getKey(), entry.getValue(), symbolTransactions));
			} else {
			    LOGGER.info(() -> entry.getKey() + " skipped because there was a SELL_PANIC recently");
			}
		}
		return minMax.values().stream().sorted((e2, e1) -> Double.compare(e1.getFactor(), e2.getFactor()))
				.collect(Collectors.toList());
	}
	
	private static double benefit(double minProfitableSellPrice, double currentPrice) {
	    if (currentPrice > minProfitableSellPrice) {
	        return 1 - (minProfitableSellPrice/currentPrice);
	    } else {
	        return -1 * (1 - (currentPrice/minProfitableSellPrice));
	    }
	}
	
	private static Broker buySellInstance(CloudProperties cloudProperties, String symbol, List<CsvRow> rows, List<CsvTransactionRow> symbolTransactions) {
		double minProfitableSellPrice = Utils.minSellProfitable(symbolTransactions);
		boolean hasPreviousTransactions = symbolTransactions != null && !symbolTransactions.isEmpty();
		CsvRow newest = rows.get(rows.size() - 1);
		Date lastPurchase = null;
		if (hasPreviousTransactions) {
		    lastPurchase = symbolTransactions.get(0).getDate();
		    LOGGER.info(() -> symbol + " is " + Utils.format(benefit(minProfitableSellPrice, newest.getPrice())) + " compared with min profitable price");
		}
		FixedBuySell fixedBuySell = cloudProperties.FIXED_BUY_SELL.get(symbol);
		CsvRow oldest = rows.get(0);
	    if (fixedBuySell != null) {
		    return new LimitsBroker(symbol, rows, fixedBuySell);
	    } else if (cloudProperties.PANIC_BROKER_ENABLE && PanicBroker.isPanic(cloudProperties, newest, minProfitableSellPrice)) {
			return new PanicBroker(symbol, newest, minProfitableSellPrice);
		} else if (cloudProperties.GREEDY_BROKER_ENABLE && newest.getPrice() > newest.getAvg2() && newest.getAvg2() > oldest.getAvg2()) {
			return new GreedyBroker(cloudProperties, symbol, rows, minProfitableSellPrice, hasPreviousTransactions, symbolTransactions);
		} else {
			return new CautelousBroker(cloudProperties, symbol, rows, minProfitableSellPrice, hasPreviousTransactions, lastPurchase);
		}
	}

	private static List<CsvTransactionRow> filterLastBuys(List<CsvTransactionRow> symbolTransactions) {
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
