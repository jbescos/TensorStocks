package com.jbescos.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;

import com.jbescos.common.Broker.Action;

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
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, Utils.UTF8));
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
	
	public static void writeCsvRows(Collection<CsvRow> csv, char separator, OutputStream output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, Utils.UTF8));
		String line = Utils.CSV_ROW_HEADER;
		writer.append(line);
		for (CsvRow row : csv) {
			writer.append(row.toCsvLine());
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

	public static List<CsvProfitRow> readCsvProfitRows(boolean skipFirst, BufferedReader reader) throws IOException {
       DateFormat format = new SimpleDateFormat(Utils.FORMAT_SECOND);
        final String SPLIT = ",";
        return readCsv(skipFirst, line -> {
            String[] columns = line.split(SPLIT);
            Date sellDate = Utils.fromString(format, columns[0]);
            Date firstBuyDate = Utils.fromString(format, columns[1]);
            String symbol = columns[2];
            String quantityBuy = columns[3];
            String quantitySell = columns[4];
            String quantityUsdtBuy = columns[5];
            String quantityUsdtSell = columns[6];
            String commissionPercentage = columns[7];
            String commissionUsdt = columns[8];
            String usdtProfit = columns[9];
            String netUsdtProfit = columns[10];
            String profitPercentage = columns[11];
            // "SELL_DATE,FIRST_BUY_DATE,SYMBOL,QUANTITY_BUY,QUANTITY_SELL,QUANTITY_USDT_BUY,QUANTITY_USDT_SELL,COMMISSION_%,COMMISSION_USDT,USDT_PROFIT,NET_USDT_PROFIT,PROFIT_%"
            CsvProfitRow row = new CsvProfitRow(firstBuyDate, sellDate, symbol, quantityBuy, quantityUsdtBuy, quantitySell, quantityUsdtSell, commissionPercentage, commissionUsdt, usdtProfit, netUsdtProfit, profitPercentage);
            return row;
        }, reader);
	}
	
	public static List<CsvProfitRow> readCsvProfitRows(BufferedReader reader) throws IOException {
	    return readCsvProfitRows(true, reader);
	}
	
	public static List<CsvAccountRow> readCsvAccountRows(boolean skipFirst, String separator, BufferedReader reader) throws IOException {
	    DateFormat format = new SimpleDateFormat(Utils.FORMAT_SECOND);
	    return readCsv(skipFirst, line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(format, columns[0]);
			String symbol = columns[1];
			CsvAccountRow row = new CsvAccountRow(date, symbol, Double.parseDouble(columns[2]), Double.parseDouble(columns[3]));
			return row;
		}, reader);
	}
	
	public static List<CsvTransactionRow> readCsvTransactionRows(boolean skipFirst, String separator, BufferedReader reader) throws IOException {
	    DateFormat format = new SimpleDateFormat(Utils.FORMAT_SECOND);
	    return readCsv(skipFirst, line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(format, columns[0]);
			String orderId = columns[1];
			String side = columns[2];
			String symbol = columns[3];
			String usdt = columns[4];
			String quantity = columns[5];
			double usdtUnit = Double.parseDouble(columns[6]);
			CsvTransactionRow row = new CsvTransactionRow(date, orderId, Action.valueOf(side), symbol, usdt, quantity, usdtUnit);
			return row;
		}, reader);
	}
	
	public static List<CsvTxSummaryRow> readCsvTxSummaryRows(boolean skipFirst, String separator, BufferedReader reader) throws IOException {
	    DateFormat format = new SimpleDateFormat(Utils.FORMAT_SECOND);
	    return readCsv(skipFirst, line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(format, columns[0]);
			String symbol = columns[1];
			double summary = Double.parseDouble(columns[2]);
			CsvTxSummaryRow row = new CsvTxSummaryRow(date, columns[0], symbol, summary);
			return row;
		}, reader);
	}
	
	public static List<CsvRow> readCsvRows(boolean skipFirst, String separator, BufferedReader reader, Date from, Date to, List<String> symbols) throws IOException {
		DateFormat format = new SimpleDateFormat(Utils.FORMAT_SECOND);
	    return readCsv(skipFirst,  line -> {
			String[] columns = line.split(separator);
			Date date = Utils.fromString(format, columns[0]);
			if (date.getTime() >= from.getTime() && date.getTime() < to.getTime()) {
				String symbol = columns[1];
				if (symbols.isEmpty() || symbols.contains(symbol)) {
					Double avg = null;
					Double longAvg = null;
					int fearGreedIndex = 50;
					if (columns.length > 3) {
						avg = Double.parseDouble(columns[3]);
						if (columns.length > 4) {
							longAvg = Double.parseDouble(columns[4]);
							if (columns.length > 5) {
								fearGreedIndex = Integer.parseInt(columns[5]);
							}
						}
					}
					CsvRow row = new CsvRow(date, symbol, Double.parseDouble(columns[2]), avg, longAvg, fearGreedIndex);
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
