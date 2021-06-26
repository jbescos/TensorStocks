package com.jbescos.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Utils;

public class CsvUpdater {
	
	private static final Logger LOGGER = Logger.getLogger(CsvUpdater.class.getName());

	public static void main(String args[]) throws IOException {
//		addAvg("/home/jbescos/workspace/TensorStocks/cloud/cloud-test/src/test/resources");
		addAvgDated("/home/jbescos/workspace/TensorStocks/cloud/cloud-test/src/test/resources");
//		revert("C:\\workspace\\TensorStocks\\cloud\\cloud-test\\src\\test\\resources");
		LOGGER.info("Finished");
	}
	
	private static void revert(String rootFolder) throws IOException {
		File rootCsvFolder = new File(rootFolder);
		String[] files = rootCsvFolder.list();
		for (String file : files) {
			if (file.endsWith(".csv") && !file.contains("reversed")) {
				String fullPath = rootFolder + "/" + file;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fullPath)))) {
					List<CsvRow> rows = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
					LOGGER.info("Read " + rows.size() + " rows in " + fullPath);
					for (int i=0; i<(rows.size()/2);i++) {
						CsvRow row0 = rows.get(i);
						Date date0 = row0.getDate();
						CsvRow rowLast = rows.get((rows.size() - 1) - i);
						row0.setDate(rowLast.getDate());
						rowLast.setDate(date0);
					}
					Collections.reverse(rows);
					Double previousResult = null;
					for (CsvRow row : rows) {
						previousResult = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), previousResult);
						row.setAvg(previousResult);
					}
					try (OutputStream output = new FileOutputStream(rootFolder + "/reversed_" + file)) {
						CsvUtil.writeCsvRows(rows, ',', output);
					}
				}
			}
		}
	}
	
	private static void addAvgDated(String rootFolder) throws IOException {
		File rootCsvFolder = new File(rootFolder);
		String[] files = rootCsvFolder.list();
		List<CsvRow> rows = new ArrayList<>();
		for (String file : files) {
			if (file.endsWith(".csv")) {
				String fullPath = rootFolder + "/" + file;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fullPath)))) {
					rows.addAll(CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList()));
					LOGGER.info("Read " + rows.size() + " rows in " + fullPath);
				}
				File f = new File(fullPath);
				f.delete();
			}
		}
		Collections.sort(rows, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));
		Map<String, List<CsvRow>> groupedSymbol = rows.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		for (List<CsvRow> values : groupedSymbol.values()) {
			Double previousResult = null;
			for (CsvRow row : values) {
				previousResult = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), previousResult);
				row.setAvg(previousResult);
			}
		}
		Map<String, List<CsvRow>> groupedDate = rows.stream().collect(Collectors.groupingBy(row -> Utils.fromDate(Utils.FORMAT, row.getDate())));
		List<String> sortedDates = new ArrayList<>(groupedDate.keySet());
		Collections.sort(sortedDates);
		List<CsvRow> dayRows = null;
		for (String date : sortedDates) {
			File updateFile = new File(rootFolder + "/" +date + ".csv");
			LOGGER.info("Creating " + updateFile);
			try (OutputStream output = new FileOutputStream(updateFile)) {
				dayRows = groupedDate.get(date);
				CsvUtil.writeCsvRows(dayRows, ',', output);
			}
		}
		Map<String, List<CsvRow>> lastDayGrouped = dayRows.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		List<CsvRow> lastRows = new ArrayList<>();
		for (Entry<String, List<CsvRow>> day : lastDayGrouped.entrySet()) {
			CsvRow lastRow = day.getValue().get(day.getValue().size() - 1);
			lastRows.add(lastRow);
		}
		try (FileOutputStream output = new FileOutputStream(rootFolder + "/" + Utils.LAST_PRICE)) {
			CsvUtil.writeCsvRows(lastRows, ',', output);
		}
	}
	
	private static void addAvg(String rootFolder) throws IOException {
		File rootCsvFolder = new File(rootFolder);
		String[] files = rootCsvFolder.list();
		for (String file : files) {
			if (file.endsWith(".csv")) {
				String fullPath = rootFolder + "/" + file;
				LOGGER.info("Processing " + fullPath);
				List<CsvRow> rows = null;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fullPath)))) {
					rows = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
				}
				Double previousResult = null;
				for (CsvRow row : rows) {
					previousResult = Utils.ewma(CloudProperties.EWMA_CONSTANT, row.getPrice(), previousResult);
					row.setAvg(previousResult);
				}
				File f = new File(fullPath);
				f.delete();
				try (OutputStream output = new FileOutputStream(fullPath)) {
					CsvUtil.writeCsvRows(rows, ',', output);
				}
			}
		}
	}
}
