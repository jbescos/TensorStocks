package es.tododev.stocks.chart;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class XYChart implements IChart {

	private final XYSeriesCollection dataset = new XYSeriesCollection();

	@Override
	public void add(String lineLabel, List<CsvRow> data) {
		XYSeries series = new XYSeries(lineLabel);
		int i = 0;
		for (CsvRow row : data) {
			series.add(i, row.getValue());
			i++;
		}
		dataset.addSeries(series);
	}

	@Override
	public void save(String filePath, String title, String horizontalLabel, String verticalLabel) throws IOException {
		JFreeChart xylineChart = ChartFactory.createXYLineChart(
				title, 
				horizontalLabel,
				verticalLabel, 
		         dataset,
		         PlotOrientation.VERTICAL, 
		         true, true, true);
		ChartUtils.saveChartAsPNG(new File(filePath), xylineChart, 1080, 1200);
	}

}
