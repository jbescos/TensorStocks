package com.jbescos.cloudchart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class ChartGenerator {

	private static final Logger LOGGER = Logger.getLogger(ChartGenerator.class.getName());

	public static void writeLoadAndWriteChart(OutputStream output, int daysBack, IChartCsv chartCsv)
			throws IOException {

		List<String> days = Utils.daysBack(new Date(), daysBack, chartCsv.prefix(), ".csv");
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		IChart<IRow> chart = create();
		List<IRow> rows = new ArrayList<>();
		Page<Blob> blobs = storage.list(CloudProperties.BUCKET, BlobListOption.prefix(chartCsv.prefix()));
		for (Blob blob : blobs.iterateAll()) {
			String fileName = blob.getName();
			if (days.contains(fileName)) {
				try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, fileName);
						BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
					List<? extends IRow> csv = chartCsv.read(reader);
					rows.addAll(csv);
				}
			}
		}
		writeChart(rows, output, chart);
	}

	public static void writeChart(List<? extends IRow> rows, OutputStream output, IChart<IRow> chart)
			throws IOException {
		Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getSymbol));
		for (Entry<String, List<IRow>> entry : grouped.entrySet()) {
			chart.add(entry.getKey(), entry.getValue());
			chart.add(entry.getKey() + "-AVG", avg(entry.getValue()));
		}
		chart.save(output, "Crypto currencies", "", "USDT");
	}

	private static List<IRow> avg(List<IRow> data) {
		List<IRow> avg = new ArrayList<>(data.size());
		for (int i = 0; i < data.size(); i++) {
			double totalPrice = 0;
			for (int j = 0; j < i; j++) {
				totalPrice = totalPrice + data.get(j).getPrice();
			}
			IRow current = data.get(i);
			IRow avgRow = new CsvRow(current.getDate(), current.getSymbol(), totalPrice / (i + 1));
			avg.add(avgRow);
		}
		return avg;
	}

	private static IChart<IRow> create() {
		if ("date".equals(CloudProperties.CHART_TYPE)) {
			return new DateChart();
		} else {
			return new XYChart();
		}
	}

	static interface IChartCsv {
		String prefix();

		List<? extends IRow> read(BufferedReader reader) throws IOException;
	}

	static class AccountChartCsv implements IChartCsv {

		@Override
		public String prefix() {
			return "account_";
		}

		@Override
		public List<? extends IRow> read(BufferedReader reader) throws IOException {
			return CsvUtil.readCsvAccountRows(true, ",", reader);
		}

	}

	static class SymbolChartCsv implements IChartCsv {

		private final List<String> symbols;

		public SymbolChartCsv(List<String> symbols) {
			this.symbols = symbols;
		}

		@Override
		public String prefix() {
			return "";
		}

		@Override
		public List<? extends IRow> read(BufferedReader reader) throws IOException {
			List<? extends IRow> rows = CsvUtil.readCsvRows(true, ",", reader);
			if (symbols != null && !symbols.isEmpty()) {
				rows = rows.stream().filter(row -> symbols.contains(row.getSymbol())).collect(Collectors.toList());
			}
			return rows;
		}

	}
}
