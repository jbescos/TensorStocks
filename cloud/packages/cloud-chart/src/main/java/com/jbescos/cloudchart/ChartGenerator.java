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
import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class ChartGenerator {

	private static final Logger LOGGER = Logger.getLogger(ChartGenerator.class.getName());
	private static final int PRECISSION_CHART_DAYS = 7;

	public static void writeLoadAndWriteChart(OutputStream output, int daysBack, IChartCsv chartCsv)
			throws IOException {
		IChart<IRow> chart = create(daysBack);
		List<IRow> rows = chartCsv.read(daysBack);
		writeChart(rows, output, chart);
		save(output, chart);
	}

	public static void writeChart(List<? extends IRow> rows, OutputStream output, IChart<IRow> chart)
			throws IOException {
		Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getLabel));
		for (Entry<String, List<IRow>> entry : grouped.entrySet()) {
			chart.add(entry.getKey(), entry.getValue());
		}
	}
	
	public static void save(OutputStream output, IChart<IRow> chart) throws IOException {
		chart.save(output, "Crypto currencies", "", "USDT");
	}

	private static IChart<IRow> create(int daysBack) {
		if (daysBack > PRECISSION_CHART_DAYS) {
			return new DateChart();
		} else {
			return new XYChart();
		}
	}

	static interface IChartCsv {

		List<IRow> read(int daysBack) throws IOException;
	}

	static class AccountChartCsv implements IChartCsv {
		
		private static final String PREFIX = "wallet/account_";
		private final Page<Blob> walletBlobs;

		public AccountChartCsv () {
			Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
			walletBlobs = storage.list(CloudProperties.BUCKET, BlobListOption.prefix(PREFIX));
		}

		@Override
		public List<IRow> read(int daysBack) throws IOException {
			List<String> days = Utils.daysBack(new Date(), daysBack, PREFIX, ".csv");
			List<IRow> rows = new ArrayList<>();
			for (Blob blob : walletBlobs.iterateAll()) {
				if (days.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						rows.addAll(CsvUtil.readCsvAccountRows(true, ",", reader));
					}
				}
			}
			return rows;
		}

	}

	static class SymbolChartCsv implements IChartCsv {

		private static final String DATA_PREFIX = "data/";
		private static final String TRANSACTIONS_PREFIX = "transactions/transactions_";
		private final List<String> symbols;
		private final Page<Blob> dataBlobs;
		private final Page<Blob> transactionBlobs;

		public SymbolChartCsv(List<String> symbols) {
			this.symbols = symbols;
			Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
			dataBlobs = storage.list(CloudProperties.BUCKET, BlobListOption.prefix(DATA_PREFIX));
			transactionBlobs = storage.list(CloudProperties.BUCKET, BlobListOption.prefix(TRANSACTIONS_PREFIX));
		}

		@Override
		public List<IRow> read(int daysBack) throws IOException {
			Date now = new Date();
			List<IRow> total = new ArrayList<>();
			List<String> days = Utils.daysBack(now, daysBack, DATA_PREFIX, ".csv");
			for (Blob blob : dataBlobs.iterateAll()) {
				if (days.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						List<? extends IRow> rows = CsvUtil.readCsvRows(true, ",", reader);
						if (symbols != null && !symbols.isEmpty()) {
							rows = rows.stream().filter(row -> symbols.contains(row.getLabel())).collect(Collectors.toList());
						}
						if (daysBack > PRECISSION_CHART_DAYS) {
							// Pick the last to avoid memory issues
							Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getLabel));
							List<IRow> lastOfEachSymbol = new ArrayList<>();
							for (List<IRow> values : grouped.values()) {
								if (!values.isEmpty()) {
									lastOfEachSymbol.add(values.get(values.size() - 1));
								}
							}
							total.addAll(lastOfEachSymbol);
						} else {
							total.addAll(rows);
						}
					}
				}
			}
			days = Utils.daysBack(now, daysBack, TRANSACTIONS_PREFIX, ".csv");
			for (Blob blob : transactionBlobs.iterateAll()) {
				if (days.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						List<CsvTransactionRow> transactions = CsvUtil.readCsvTransactionRows(true, ",", reader);
						if (symbols != null && !symbols.isEmpty()) {
							transactions = transactions.stream().filter(row -> symbols.contains(row.getSymbol())).collect(Collectors.toList());
						}
						transactions.stream().forEach(tx -> tx.setUsdt(tx.getUsdtUnit()));
						total.addAll(transactions);
					}
				}
			}
			return total;
		}

	}
}
