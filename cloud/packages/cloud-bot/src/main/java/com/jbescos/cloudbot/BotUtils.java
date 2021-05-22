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
import java.util.stream.Collectors;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.Utils;

public class BotUtils {

	public static List<SymbolStats> loadStatistics(int daysBack) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		List<String> days = Utils.daysBack(new Date(), daysBack, "", ".csv");
		List<CsvRow> rows = new ArrayList<>();
		for (String day : days) {
			try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, day);
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				List<CsvRow> csv = CsvUtil.readCsvRows(true, ",", reader);
				rows.addAll(csv);
			}
		}
		Date now = new Date();
		List<CsvRow> latestCsv = BinanceAPI.price().stream()
				.map(price -> new CsvRow(now, price.getSymbol(), price.getPrice()))
				.collect(Collectors.toList());
		rows.addAll(latestCsv);
		return fromCsvRows(rows);
	}

	public static List<SymbolStats> fromCsvRows(List<CsvRow> csv) {
		Map<String, SymbolStats> minMax = new LinkedHashMap<>();
		Map<String, List<CsvRow>> grouped = csv.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			minMax.put(entry.getKey(), new SymbolStats(entry.getKey(), entry.getValue()));
		}
		return minMax.values().stream().sorted((e2, e1) -> Double.compare(e1.getFactor(), e2.getFactor()))
				.collect(Collectors.toList());
	}

}
