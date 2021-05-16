package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Supplier;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class XYChart implements IChart<Double> {

	private final XYSeriesCollection dataset = new XYSeriesCollection();

	@Override
	public void add(String lineLabel, List<Supplier<Double>> data) {
		XYSeries series = new XYSeries(lineLabel);
		int i = 0;
		for (Supplier<Double> row : data) {
			series.add(i, row.get());
			i++;
		}
		dataset.addSeries(series);
	}

	@Override
	public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel) throws IOException {
		JFreeChart xylineChart = ChartFactory.createXYLineChart(
				title, 
				horizontalLabel,
				verticalLabel, 
		         dataset,
		         PlotOrientation.VERTICAL, 
		         true, true, true);
		BufferedImage image = xylineChart.createBufferedImage(1080, 1200);
		ChartUtils.writeBufferedImageAsPNG(output, image);
	}

}
