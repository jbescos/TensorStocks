package com.jbescos.cloudbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Utils;

public class BotUtils {
	
	private static final String BUCKET;
	private static final String PROJECT_ID;
	// FIXME change it by the predictions file
	private static final String PREDICTIONS = "total.csv";

	static {
		try {
			Properties properties = Utils.fromClasspath("/storage.properties");
			BUCKET = properties.getProperty("storage.bucket");
			PROJECT_ID = properties.getProperty("project.id");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static List<SymbolStats> loadPredictions(Date now) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
		try (ReadChannel readChannel = storage.reader(BUCKET, PREDICTIONS);
				BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
			return loadPredictions(now, new Date(Long.MAX_VALUE), reader);
		}
	}

	public static List<SymbolStats> loadPredictions(Date from, Date to, BufferedReader reader) throws IOException {
		List<CsvRow> csv = CsvUtil.readCsvRows(true, ",", reader, from, to);
		Map<String, SymbolStats> minMax = new LinkedHashMap<>();
		Map<String, List<CsvRow>> grouped = csv.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			minMax.put(entry.getKey(), new SymbolStats(entry.getKey(), entry.getValue()));
		}
		return minMax.values().stream().sorted((e2, e1) -> Double.compare(e1.getFactor(), e2.getFactor())).collect(Collectors.toList());
	}
	
}
