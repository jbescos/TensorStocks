package es.tododev.stocks;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class Chart {

	private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	private int width; 

	public void add(String lineLabel, List<Pair<Number, String>> data) {
		Set<String> horizontal = new HashSet<>();
		for (Pair<Number, String> pair : data) {
			horizontal.add(pair.getM());
			dataset.addValue(pair.getN(), lineLabel, pair.getM());
		}
		if (width < horizontal.size()) {
			width = horizontal.size();
		}
	}

	public void save(String filePath, String title, String horizontalLabel, String verticalLabel) throws IOException {
		JFreeChart histogram = ChartFactory.createLineChart(title, horizontalLabel, verticalLabel, dataset,
				PlotOrientation.VERTICAL, true, true, true);
		ChartUtils.saveChartAsPNG(new File(filePath), histogram, width * 100, 1200);
	}

}
