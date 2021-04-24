package es.tododev.stocks.chart;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
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
//		IChart chart = new LineChart(dateFormat);
		IChart chart = new XYChart();
		if (!csvRootFolder.isDirectory()) {
			throw new IllegalArgumentException(csvRootFolder.getAbsolutePath() + " is not a directory");
		}
		File[] symbols = csvRootFolder.listFiles();
		for (File symbol : symbols) {
			if (symbol.isDirectory()) {
				List<CsvRow> total = new LinkedList<>();
				for (File child : symbol.listFiles()) {
					if (child.getName().endsWith(".csv")) {
						total.addAll(CsvParser.getRows(child, from, to));
					}
				}
				Collections.sort(total, (row1, row2) -> row1.getDate().compareTo(row2.getDate()));
				chart.add(symbol.getName(), total);
			}
		}
		chart.save(csvRootFolder.getAbsolutePath() + "/chart.png", "Summary", "From " + format.format(from) + " to " + format.format(to), "USD");
	}
	
}
