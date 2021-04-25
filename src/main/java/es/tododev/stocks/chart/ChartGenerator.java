package es.tododev.stocks.chart;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ChartGenerator {

	private final File csvRootFolder;
	
	public ChartGenerator(File csvRootFolder) {
		this.csvRootFolder = csvRootFolder;
	}
	
	public void generateChart(Date from, Date to, String dateFormat) throws IOException {
		DateFormat format = new SimpleDateFormat(dateFormat);

		if (!csvRootFolder.isDirectory()) {
			throw new IllegalArgumentException(csvRootFolder.getAbsolutePath() + " is not a directory");
		}
		File[] symbols = csvRootFolder.listFiles();
		for (File symbol : symbols) {
			if (symbol.isDirectory()) {
//				IChart chart = new LineChart(dateFormat);
				IChart chart = new XYChart();
				addInChart(chart, symbol, from, to, CsvColumns.adjclose);
				addInChart(chart, symbol, from, to, CsvColumns.open);
				addInChart(chart, symbol, from, to, CsvColumns.high);
				addInChart(chart, symbol, from, to, CsvColumns.low);
				addInChart(chart, symbol, from, to, CsvColumns.close);
				chart.save(csvRootFolder.getAbsolutePath() + "/" + symbol.getName() + ".png", symbol.getName(), "From " + format.format(from), "USD");
			}
		}
	}
	
	private void addInChart(IChart chart, File symbol, Date from, Date to, CsvColumns column) throws IOException {
		List<CsvRow> total = new LinkedList<>();
		for (File child : symbol.listFiles()) {
			if (child.getName().endsWith(".csv")) {
				total.addAll(CsvParser.getRows(child, from, to, column.getColumnIdx()));
			}
		}
		chart.add(symbol.getName() + "-" + column.name(), total);
	}
	
}
