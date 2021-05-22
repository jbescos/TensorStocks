package com.jbescos.cloudchart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class ChartGenerator {

	private static final String TOTAL_FILE = "total.csv";
	private static final String ACCOUNT_TOTAL_FILE = "account_total.csv";

	public static void writePricesChart(OutputStream output) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		IChart<IRow> chart = create();
		try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, TOTAL_FILE);
				BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
			List<? extends IRow> csv = CsvUtil.readCsvRows(true, ",", reader);
			writeChart(csv, output, chart);
		}
	}
	
	public static void writeAccountChart(OutputStream output) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		IChart<IRow> chart = create();
		try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, ACCOUNT_TOTAL_FILE);
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
	
	private static IChart<IRow> create(){
		if ("date".equals(CloudProperties.CHART_TYPE)) {
			return new DateChart();
		} else {
			return new XYChart();
		}
	}

}
