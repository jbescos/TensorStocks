package com.jbescos.cloudchart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.exchange.CsvAccountRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.CsvTxSummaryRow;
import com.jbescos.exchange.IRow;
import com.jbescos.exchange.Utils;

public class ChartGenerator {

	private static final Logger LOGGER = Logger.getLogger(ChartGenerator.class.getName());
	private static final String DATA_PREFIX = "data";
	private static final int PRECISSION_CHART_DAYS = 7;

	public static void writeLoadAndWriteChart(OutputStream output, int daysBack, IChartCsv chartCsv)
			throws IOException {
		IChart<IRow> chart = chartCsv.chart(daysBack);
		List<IRow> rows = chartCsv.read(daysBack);
		writeChart(rows, output, chart);
		save(output, chart);
	}

	public static void writeChart(List<? extends IRow> rows, OutputStream output, IChart<IRow> chart)
			throws IOException {
		Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getLabel));
		List<String> keys = new ArrayList<>(grouped.keySet());
		Utils.sortForChart(keys);
		LOGGER.info(() -> "Chart display order " + keys);
		for (String key : keys) {
			chart.add(key, grouped.get(key));
		}
	}
	
	public static void save(OutputStream output, IChart<?> chart) throws IOException {
		chart.save(output, "Crypto currencies", "", "USDT");
	}

	static interface IChartCsv {

		List<IRow> read(int daysBack) throws IOException;
		
		IChart<IRow> chart(int daysBack);
	}
	
	static class ProfitableBarChartCsv implements IChartCsv {

		private final Page<Blob> transactionBlobs;
		private final List<String> symbols;
		private final Map<String, Double> wallet = new LinkedHashMap<>();
		private final Date now = new Date();
		private final CloudProperties cloudProperties;
		
		public ProfitableBarChartCsv(CloudProperties cloudProperties, List<String> symbols) throws IOException {
			this.cloudProperties = cloudProperties;
			this.symbols = symbols;
			Storage storage = StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService();
			String todaysWalletCsv = cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv";
			Blob retrieve = storage.get(cloudProperties.BUCKET, todaysWalletCsv);
			try (ReadChannel readChannel = retrieve.reader();
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				List<CsvAccountRow> account = CsvUtil.readCsvAccountRows(true, ",", reader);
				LOGGER.info(() -> "Loaded from wallet " + account.size() + " from " + todaysWalletCsv);
				Map<Date, List<CsvAccountRow>> byDate = account.stream().collect(Collectors.groupingBy(CsvAccountRow::getDate));
				Date max = Collections.max(byDate.keySet());
				Map<String, List<CsvAccountRow>> grouped = byDate.get(max).stream().collect(Collectors.groupingBy(IRow::getLabel));
				for (Entry<String, List<CsvAccountRow>> entry : grouped.entrySet()) {
					if (!entry.getValue().isEmpty()) {
						wallet.put(entry.getKey() + Utils.USDT, entry.getValue().get(entry.getValue().size() - 1).getPrice());
					}
					
				}
			}
			transactionBlobs = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX));
		}
		
		@Override
		public List<IRow> read(int daysBack) throws IOException {
		    Date from = Utils.getDateOfDaysBack(now, daysBack);
			List<IRow> total = new ArrayList<>();
			List<String> months = Utils.monthsBack(now, (daysBack / 31) + 2, cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
			for (Blob blob : transactionBlobs.iterateAll()) {
				if (months.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						List<CsvTransactionRow> transactions = CsvUtil.readCsvTransactionRows(true, ",", reader).stream().filter(row -> row.getDate().getTime() >= from.getTime()).collect(Collectors.toList());
						if (symbols != null && !symbols.isEmpty()) {
							transactions = transactions.stream().filter(row -> symbols.contains(row.getSymbol())).collect(Collectors.toList());
						}
						total.addAll(transactions);
					}
				}
			}
			return total;
		}

		@Override
		public IChart<IRow> chart(int daysBack) {
			return new BarChart(wallet);
		}
		
	}

	static class AccountChartCsv implements IChartCsv {
		
		private final Page<Blob> walletBlobs;
		private final CloudProperties cloudProperties;

		public AccountChartCsv (CloudProperties cloudProperties) {
			this.cloudProperties = cloudProperties;
			Storage storage = StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService();
			walletBlobs = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX));
		}

		@Override
		public List<IRow> read(int daysBack) throws IOException {
			Date now = new Date();
			Date from = Utils.getDateOfDaysBack(now, daysBack);
			List<String> months = Utils.monthsBack(now, (daysBack / 31) + 2, cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX, ".csv");
			List<IRow> rows = new ArrayList<>();
			for (Blob blob : walletBlobs.iterateAll()) {
				if (months.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						// Exclude currencies with little value
						List<CsvAccountRow> rowsInMonth = CsvUtil.readCsvAccountRows(true, ",", reader).stream().filter(row -> row.getDate().getTime() >= from.getTime()).filter(row -> row.getPrice() > Utils.MIN_WALLET_VALUE_TO_RECORD).collect(Collectors.toList());
						rows.addAll(rowsInMonth);
					}
				}
			}
			return rows;
		}

		@Override
		public IChart<IRow> chart(int daysBack) {
			if (daysBack > PRECISSION_CHART_DAYS) {
				return new DateChart();
			} else {
				return new XYChart();
			}
		}

	}
	
	static class TxSummaryChartCsv implements IChartCsv {
		
		private final Page<Blob> txSummaryBlobs;
		private final CloudProperties cloudProperties;
		private final List<String> symbols;

		public TxSummaryChartCsv(CloudProperties cloudProperties, List<String> symbols) {
			this.cloudProperties = cloudProperties;
			this.symbols = symbols == null ? Collections.emptyList() : symbols;
			Storage storage = StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService();
			txSummaryBlobs = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(cloudProperties.USER_ID + "/" + Utils.TX_SUMMARY_PREFIX));
		}

		@Override
		public List<IRow> read(int daysBack) throws IOException {
			Date now = new Date();
			List<IRow> total = new ArrayList<>();
			List<String> days = Utils.daysBack(now, daysBack, cloudProperties.USER_ID + "/" + Utils.TX_SUMMARY_PREFIX, ".csv");
			for (Blob blob : txSummaryBlobs.iterateAll()) {
				if (days.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						List<? extends IRow> rows = CsvUtil.readCsvTxSummaryRows(true, ",", reader).stream().filter(row -> {
							if (!symbols.isEmpty() && !symbols.contains(row.getLabel())) {
								return false;
							} else {
								return true;
							}
						}).collect(Collectors.toList());
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
			Optional<IRow> min = total.stream().min((r1, r2) -> r1.getDate().compareTo(r2.getDate()));
			if (min.isPresent()) {
				// Set a line in the zero
				final String ZERO = "ZERO";
				CsvTxSummaryRow zero0 = new CsvTxSummaryRow(min.get().getDate(), Utils.fromDate(Utils.FORMAT_SECOND, min.get().getDate()), ZERO, 0);
				CsvTxSummaryRow zero1 = new CsvTxSummaryRow(now, Utils.fromDate(Utils.FORMAT_SECOND, now), ZERO, 0);
				total.add(zero0);
				total.add(zero1);
			}
			return total;
		}

		@Override
		public IChart<IRow> chart(int daysBack) {
			if (daysBack > PRECISSION_CHART_DAYS) {
				return new DateChart();
			} else {
				return new XYChart();
			}
		}
		
	}

	static class SymbolChartCsv implements IChartCsv {

		private final List<String> symbols;
		private final Page<Blob> dataBlobs;
		private final Page<Blob> transactionBlobs;
		private final CloudProperties cloudProperties;

		public SymbolChartCsv(CloudProperties cloudProperties, List<String> symbols) {
			this.cloudProperties = cloudProperties;
			this.symbols = symbols;
			Storage storage = StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService();
			dataBlobs = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(DATA_PREFIX + cloudProperties.USER_EXCHANGE.getFolder()));
			transactionBlobs = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX));
		}

		@Override
		public IChart<IRow> chart(int daysBack) {
			if (daysBack > PRECISSION_CHART_DAYS) {
				return new DateChart();
			} else {
				return new XYChart();
			}
		}
		
		@Override
		public List<IRow> read(int daysBack) throws IOException {
			Date now = new Date();
			List<IRow> total = new ArrayList<>();
			List<String> days = Utils.daysBack(now, daysBack, DATA_PREFIX + cloudProperties.USER_EXCHANGE.getFolder(), ".csv");
			for (Blob blob : dataBlobs.iterateAll()) {
				if (days.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						List<? extends IRow> rows = CsvUtil.readCsvRows(true, ",", reader, symbols);
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
			Date from = Utils.getDateOfDaysBack(now, daysBack);
			List<String> months = Utils.monthsBack(now, (daysBack / 31) + 2, cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
			LOGGER.info(() -> "Loading transactions of " + months);
			for (Blob blob : transactionBlobs.iterateAll()) {
				if (months.contains(blob.getName())) {
					try (ReadChannel readChannel = blob.reader();
							BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
						List<CsvTransactionRow> transactions = CsvUtil.readCsvTransactionRows(true, ",", reader).stream().filter(row -> row.getDate().getTime() >= from.getTime()).collect(Collectors.toList());
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
