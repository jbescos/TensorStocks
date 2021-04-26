package es.tododev.stocks.yahoo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import es.tododev.stocks.utils.Utils;

public class CsvGenerator {

	private static final DateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");
	private final YahooAPI yahooAPI;

	public CsvGenerator(String yahooApiKey) {
		yahooAPI = new YahooAPI(yahooApiKey);
	}

	public void generateCsv(Map<String, String> symbols, File resultFolder) throws IOException {
		String currentMonth = MONTH_FORMAT.format(new Date());
		for (Entry<String, String> entry : symbols.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			int symbolId = Utils.getSymbol(value);
			File folder = new File(resultFolder, key);
			try {
				folder.mkdirs();
				Prices prices = yahooAPI.getHistory(value);
				Map<String, List<PriceItem>> grouped = prices.getPrices().stream().collect(
						Collectors.groupingBy(item -> MONTH_FORMAT.format(Utils.fromTimestamp(item.getDate()))));
				for (Entry<String, List<PriceItem>> perMonth : grouped.entrySet()) {
					StringBuilder builder = new StringBuilder();
					builder.append("date,adjclose,open,high,low,close,volume,symbol\n");
					for (PriceItem item : perMonth.getValue()) {
						builder.append(item.getDate()).append(",").append(item.getAdjclose()).append(",")
								.append(item.getOpen()).append(",").append(item.getHigh()).append(",")
								.append(item.getLow()).append(",").append(item.getClose()).append(",")
								.append(item.getVolume()).append(",").append(symbolId).append("\n");
					}
					File resultsFile = new File(folder, perMonth.getKey() + ".csv");
					// Write file if does not exist or it is the current month
					if (!resultsFile.exists() || currentMonth.equals(perMonth.getKey())) {
						Utils.writeInFile(resultsFile, builder.toString());
					}
				}
			} catch (Exception e) {
				File error = new File(folder, "error.log");
				try (PrintStream output = new PrintStream(error)) {
					e.printStackTrace(output);
				}
			}
		}
	}
}
