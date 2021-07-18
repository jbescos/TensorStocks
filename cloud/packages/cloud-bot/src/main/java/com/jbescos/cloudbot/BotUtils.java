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

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BuySellAnalisys;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.Utils;

public class BotUtils {
	
	private static final Logger LOGGER = Logger.getLogger(BotUtils.class.getName());

	public static List<BuySellAnalisys> loadStatistics(Client client, boolean requestLatestPrices) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		// Get 1 day more and compare dates later
		List<String> days = Utils.daysBack(new Date(), CloudProperties.BOT_DAYS_BACK_STATISTICS + 1, "data/", ".csv");
		List<CsvRow> rows = new ArrayList<>();
		Date now = new Date();
		Date from = Utils.getDateOfDaysBack(now, CloudProperties.BOT_DAYS_BACK_STATISTICS);
		List<CsvRow> csvInDay = null;
		for (String day : days) {
			try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, day);
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				csvInDay = CsvUtil.readCsvRows(true, ",", reader, CloudProperties.BOT_WHITE_LIST_SYMBOLS);
				csvInDay = csvInDay.stream().filter(row -> row.getDate().getTime() > from.getTime()).collect(Collectors.toList());
				rows.addAll(csvInDay);
				LOGGER.info("Loaded " + csvInDay.size() + " rows from " + day);
			}
		}
		if (!rows.isEmpty()) {
			LOGGER.info("Data is obtained from " + Utils.fromDate(Utils.FORMAT_SECOND, rows.get(0).getDate()) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, now));
		}
		List<CsvTransactionRow> transactions = new ArrayList<>();
		days = Utils.daysBack(new Date(), CloudProperties.BOT_DAYS_BACK_TRANSACTIONS, "transactions/transactions_", ".csv");
		for (String day : days) {
			Blob blob = storage.get(CloudProperties.BUCKET, day);
			if (blob != null) {
				try (ReadChannel readChannel = blob.reader();
						BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
					List<CsvTransactionRow> csv = CsvUtil.readCsvTransactionRows(true, ",", reader);
					transactions.addAll(csv);
				}
			}
		}
		LOGGER.info("Transactions loaded: " + transactions.size());
		if (requestLatestPrices) {
    		List<CsvRow> latestCsv = new BinanceAPI(client).price().stream()
    				.map(price -> new CsvRow(now, price.getSymbol(), price.getPrice())).filter(row -> CloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(row.getSymbol()))
    				.collect(Collectors.toList());
    		for (CsvRow last : latestCsv) {
    			for (CsvRow inDay : csvInDay) {
    				if (last.getSymbol().equals(inDay.getSymbol())) {
    					last.setAvg(Utils.ewma(CloudProperties.EWMA_CONSTANT, last.getPrice(), inDay.getAvg()));
    					last.setAvg2(Utils.ewma(CloudProperties.EWMA_2_CONSTANT, last.getPrice(), inDay.getAvg2()));
    					break;
    				}
    			}
    		}
    		rows.addAll(latestCsv);
		}
		return fromCsvRows(rows, transactions);
	}

	public static List<BuySellAnalisys> fromCsvRows(List<CsvRow> csv, List<CsvTransactionRow> transactions) {
		Map<String, BuySellAnalisys> minMax = new LinkedHashMap<>();
		Map<String, List<CsvRow>> grouped = csv.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		Map<String, List<CsvTransactionRow>> groupedTransactions = transactions.stream().collect(Collectors.groupingBy(CsvTransactionRow::getSymbol));
		LOGGER.info("There is data for: " + grouped.keySet());
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			minMax.put(entry.getKey(), new SymbolStats(entry.getKey(), entry.getValue(), groupedTransactions.get(entry.getKey())));
		}
		return minMax.values().stream().sorted((e2, e1) -> Double.compare(e1.getFactor(), e2.getFactor()))
				.collect(Collectors.toList());
	}

}
