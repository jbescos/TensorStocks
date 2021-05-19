package com.jbescos.cloudchart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class ChartGenerator {

	private static final String BUCKET;
	private static final String PROJECT_ID;
	private static final String TOTAL_FILE = "total.csv";
	private static final String ACCOUNT_TOTAL_FILE = "account_total.csv";

	static {
		try {
			Properties properties = Utils.fromClasspath("/storage.properties");
			BUCKET = properties.getProperty("storage.bucket");
			PROJECT_ID = properties.getProperty("project.id");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void writePricesChart(OutputStream output) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
		IChart<IRow> chart = new XYChart();
		try (ReadChannel readChannel = storage.reader(BUCKET, TOTAL_FILE);
				BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
			List<? extends IRow> csv = CsvUtil.readCsvRows(true, ",", reader);
			writeChart(csv, output, chart);
		}
	}
	
	public static void writeAccountChart(OutputStream output) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
		IChart<IRow> chart = new XYChart();
		try (ReadChannel readChannel = storage.reader(BUCKET, ACCOUNT_TOTAL_FILE);
				BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
			List<? extends IRow> csv = CsvUtil.readCsvAccountRows(true, ",", reader);
			writeChart(csv, output, chart);
		}
	}
	
	public static void writeChart(List<? extends IRow> rows, OutputStream output, IChart<IRow> chart) throws IOException {
		Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getSymbol));
		for (Entry<String, List<IRow>> entry : grouped.entrySet()) {
			chart.add(entry.getKey(), entry.getValue());
		}
		chart.save(output, "Crypto currencies", "", "USDT");
	}

}
