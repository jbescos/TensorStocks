package es.tododev.stocks.chart;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class LineChart implements IChart {

	private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	private final DateFormat dateFormat;
	private int width;
	
	public LineChart(String dateFormat) {
		this.dateFormat = new SimpleDateFormat(dateFormat);
	}

	@Override
	public void add(String lineLabel, List<CsvRow> data) {
		Set<String> horizontal = new HashSet<>();
		for (CsvRow row : data) {
			String formatted = dateFormat.format(row.getDate());
			horizontal.add(formatted);
			dataset.addValue(row.getValue(), lineLabel, formatted);
		}
		if (width < horizontal.size()) {
			width = horizontal.size();
		}
	}

	@Override
	public void save(String filePath, String title, String horizontalLabel, String verticalLabel) throws IOException {
		JFreeChart histogram = ChartFactory.createLineChart(title, horizontalLabel, verticalLabel, dataset,
				PlotOrientation.VERTICAL, true, true, true);
		ChartUtils.saveChartAsPNG(new File(filePath), histogram, width * 100, 1200);
	}

}
