package com.jbescos.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BinanceAPI.Interval;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Kline;
import com.jbescos.common.Utils;

public class CsvUpdater {
	
	private static final Logger LOGGER = Logger.getLogger(CsvUpdater.class.getName());
	private static final long MILLIS_24H = 3600 * 1000 * 24;

	public static void main(String args[]) throws IOException {
		updateCsv("C:\\workspace\\TensorStocks\\cloud\\cloud-test\\src\\\\test\\resources", "2021-05-08.csv");
		LOGGER.info("Finished");
	}
	
	private static Kline getKline(CsvRow row, List<Kline> klines) {
		long rowTimestamp = row.getDate().getTime();
		for (Kline kline : klines) {
			if (kline.getOpenTime() <= rowTimestamp && kline.getCloseTime() >= rowTimestamp) {
				return kline;
			}
		}
		return null;
	}
	
	private static void addKlines(BinanceAPI api, Map<String, List<CsvRow>> groupedSymbol, Date startTime, Date endTime) {
		for (Entry<String, List<CsvRow>> symbolRows : groupedSymbol.entrySet()) {
			List<Kline> klines = api.klines(Interval.MINUTES_30, symbolRows.getKey(), 1000, startTime.getTime(), endTime.getTime());
			for (CsvRow row : symbolRows.getValue()) {
				Kline kline = getKline(row, klines);
				row.setKline(kline);
			}
		}
	}
	
	private static void updateLastPricesFromPreviousDay(Map<String, CsvRow> lastPrices, Date startTime, String rootFolder) throws FileNotFoundException, IOException {
		String csvBefore = Utils.daysBack(startTime, 2, "", ".csv").get(0);
		LOGGER.info("Trying to load lastPrices from the CSV " + csvBefore);
		String fullPath = rootFolder + "/" + csvBefore;
		File file = new File(fullPath);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				List<CsvRow> rowsInFile = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
				Collections.sort(rowsInFile, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));
				Map<String, List<CsvRow>> groupedSymbol = rowsInFile.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
				for (List<CsvRow> values : groupedSymbol.values()) {
					CsvRow last = values.get(values.size() - 1);
					lastPrices.put(last.getSymbol(), last);
				}
			}
		} else {
			LOGGER.warning("LastPrices cannot be obtained because there is no file " + fullPath);
		}
	}
	
	private static void updateCsv(String rootFolder, String startIn) throws IOException {
		Client client = ClientBuilder.newClient();
		BinanceAPI api = new BinanceAPI(client);
		File rootCsvFolder = new File(rootFolder);
		List<String> files = new ArrayList<>(Arrays.asList(rootCsvFolder.list()));
		Collections.sort(files);
		Map<String, CsvRow> lastPrices = new HashMap<>();
		boolean startCsvFound = false;
		for (String file : files) {
			if (startCsvFound || startIn.equals(file)) {
				startCsvFound = true;
				LOGGER.info("Processing " + file);
				try {
					Date startTime = Utils.fromString(Utils.FORMAT, file.replace(".csv", ""));
					Date endTime = new Date(startTime.getTime() + MILLIS_24H);
					if (lastPrices.isEmpty()) {
						updateLastPricesFromPreviousDay(lastPrices, startTime, rootFolder);
					}
					String fullPath = rootFolder + "/" + file;
					List<CsvRow> rowsInFile = null;
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fullPath)))) {
						rowsInFile = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
						LOGGER.info("Read " + rowsInFile.size() + " rows in " + fullPath);
						Collections.sort(rowsInFile, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));
						Map<String, List<CsvRow>> groupedSymbol = rowsInFile.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
//						addKlines(api, groupedSymbol, startTime, endTime);
						for (List<CsvRow> values : groupedSymbol.values()) {
							CsvRow last = lastPrices.get(values.get(0).getSymbol());
							for (CsvRow row : values) {
								double avg;
								double avg2;
								if (last == null) {
									last = row;
									avg = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), null);
									avg2 = Utils.ewma(CloudProperties.EWMA_2_CONSTANT, row.getPrice(), null);
								} else {
									avg = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), last.getAvg());
									avg2 = Utils.ewma(CloudProperties.EWMA_2_CONSTANT, row.getPrice(), last.getAvg2());
								}
								row.setAvg(avg);
								row.setAvg2(avg2);
								last = row;
							}
							lastPrices.put(last.getSymbol(), last);
						}
					}
					File f = new File(fullPath);
					f.delete();
					try (OutputStream output = new FileOutputStream(f)) {
						CsvUtil.writeCsvRows(rowsInFile, ',', output);
					}
					LOGGER.info(f + " has been created ");
				} catch (IllegalArgumentException e) {
					LOGGER.warning(file + " is discarded");
				}
			}
		}
		File last = new File(rootFolder + "/last_price.csv");
		try (FileOutputStream output = new FileOutputStream(last)) {
			CsvUtil.writeCsvRows(lastPrices.values(), ',', output);
		}
	}

}
