package com.jbescos.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;

import com.jbescos.common.BuySellAnalisys.Action;

public class CsvUtil {

	private static final Logger LOGGER = Logger.getLogger(CsvUtil.class.getName());
	private static final Date MIN_DATE = new Date(0);
	private static final Date MAX_DATE = new Date(Long.MAX_VALUE);

	public static StringBuilder toString(List<Map<String, String>> rows) {
		StringBuilder builder = new StringBuilder();
		boolean firstInRow = true;
		for (Map<String, String> row : rows) {
			for (String value : row.values()) {
				if (!firstInRow) {
					builder.append(",");
				} else {
					firstInRow = false;
				}
				builder.append(value);
			}
			builder.append("\r\n");
			firstInRow = true;
		}
		return builder;
	}

	public static void writeCsv(List<Map<String, Object>> csv, char separator, OutputStream output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
		for (Map<String, Object> row : csv) {
			StringBuilder builder = new StringBuilder();
			for (Entry<String, Object> entry : row.entrySet()) {
				if (builder.length() != 0) {
					builder.append(separator);
				}
				Object value = entry.getValue();
				if (value instanceof Date) {
					builder.append(((Date)value).getTime()/1000);
				} else {
					builder.append(value.toString());
				}
			}
			writer.append(builder.toString());
			writer.newLine();
			writer.flush();
		}
	}
	
	public static void writeCsvRows(List<CsvRow> csv, char separator, OutputStream output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
		String line = Utils.CSV_ROW_HEADER;
		writer.append(line);
		for (CsvRow row : csv) {
			StringBuilder builder = new StringBuilder();
			builder.append(Utils.fromDate(Utils.FORMAT_SECOND, row.getDate())).append(",").append(row.getSymbol()).append(",").append(row.getPrice()).append(",").append(row.getAvg()).append(",").append(row.getAvg2());
			writer.append(builder.toString());
			writer.newLine();
		}
		writer.flush();
	}

	public static <T> List<T> readCsv(boolean skipFirst, Function<String, T> mapper, BufferedReader reader) throws IOException {
		List<T> content = new ArrayList<>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (skipFirst) {
				skipFirst = false;
			} else {
				T row = mapper.apply(line);
				if (row != null) {
					content.add(row);
				}
			}
		}
		return content;
	}
	
	public static List<String> readLines(boolean skipFirst, BufferedReader reader) throws IOException {
		return readCsv(skipFirst, line -> line, reader);
		
	}
	
	public static List<CsvRow> readCsvRows(boolean skipFirst, String separator, BufferedReader reader, List<String> symbols) throws IOException {
		return readCsvRows(skipFirst, separator, reader, MIN_DATE, MAX_DATE, symbols);
		
	}
	
	public static List<CsvAccountRow> readCsvAccountRows(boolean skipFirst, String separator, BufferedReader reader) throws IOException {
		return readCsv(skipFirst, line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(Utils.FORMAT_SECOND, columns[0]);
			String symbol = columns[1];
			CsvAccountRow row = new CsvAccountRow(date, symbol, Double.parseDouble(columns[2]), Double.parseDouble(columns[3]));
			return row;
		}, reader);
	}
	
	public static List<CsvTransactionRow> readCsvTransactionRows(boolean skipFirst, String separator, BufferedReader reader) throws IOException {
		return readCsv(skipFirst, line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(Utils.FORMAT_SECOND, columns[0]);
			String orderId = columns[1];
			String side = columns[2];
			String symbol = columns[3];
			double usdt = Double.parseDouble(columns[4]);
			double quantity = Double.parseDouble(columns[5]);
			double usdtUnit = Double.parseDouble(columns[6]);
			CsvTransactionRow row = new CsvTransactionRow(date, orderId, Action.valueOf(side), symbol, usdt, quantity, usdtUnit);
			return row;
		}, reader);
	}
	
	public static List<CsvRow> readCsvRows(boolean skipFirst, String separator, BufferedReader reader, Date from, Date to, List<String> symbols) throws IOException {
		return readCsv(skipFirst,  line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(Utils.FORMAT_SECOND, columns[0]);
			if (date.getTime() >= from.getTime() && date.getTime() < to.getTime()) {
				String symbol = columns[1];
				if (symbols.isEmpty() || symbols.contains(symbol)) {
					Double avg = null;
					Double longAvg = null;
					if (columns.length > 3) {
						avg = Double.parseDouble(columns[3]);
						if (columns.length > 4) {
							longAvg = Double.parseDouble(columns[4]);
						}
					}
					CsvRow row = new CsvRow(date, symbol, Double.parseDouble(columns[2]), avg, longAvg);
					return row;
				}
			}
			return null;
		}, reader);
		
	}
	
	public static CsvContent readCsvContent(boolean skipFirst, BufferedReader reader) throws IOException {
		CsvContent content = new CsvContent();
		List<String> lines = readLines(skipFirst, reader);
		lines.stream().forEach(line -> content.add(line));
		return content;
	}
}
