package com.jbescos.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;

public class CsvUtil {

	private static final Logger LOGGER = Logger.getLogger(CsvUtil.class.getName());

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
		LOGGER.info("Rows read:  " + content.size());
		return content;
	}
	
	public static List<String> readLines(boolean skipFirst, BufferedReader reader) throws IOException {
		return readCsv(skipFirst, line -> line, reader);
		
	}
	
	public static List<CsvRow> readCsvRows(boolean skipFirst, String separator, BufferedReader reader) throws IOException {
		return readCsvRows(skipFirst, separator, reader, new Date(0), new Date(Long.MAX_VALUE));
		
	}
	
	public static List<CsvRow> readCsvRows(boolean skipFirst, String separator, BufferedReader reader, Date from, Date to) throws IOException {
		return readCsv(skipFirst,  line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(Utils.FORMAT_SECOND, columns[0]);
			if (date.getTime() >= from.getTime() && date.getTime() < to.getTime()) {
				String symbol = columns[1];
				CsvRow row = new CsvRow(date, symbol, Double.parseDouble(columns[2]));
				return row;
			} else {
				return null;
			}
		}, reader);
		
	}
	
	public static CsvContent readCsvContent(boolean skipFirst, BufferedReader reader) throws IOException {
		CsvContent content = new CsvContent();
		List<String> lines = readLines(skipFirst, reader);
		lines.stream().forEach(line -> content.add(line));
		return content;
	}
}
