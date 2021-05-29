package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.jbescos.common.IRow;

public class XYChart implements IChart<IRow> {

	private final XYSeriesCollection dataset = new XYSeriesCollection();

	@Override
	public void add(String lineLabel, List<? extends IRow> data) {
		XYSeries series = new XYSeries(lineLabel);
		for (IRow row : data) {
			series.add(row.getDate().getTime(), row.getPrice());
		}
		dataset.addSeries(series);
	}

	@Override
	public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel)
			throws IOException {
		JFreeChart xylineChart = ChartFactory.createXYLineChart(title, horizontalLabel, verticalLabel, dataset,
				PlotOrientation.VERTICAL, true, true, true);
		XYPlot xyplot = xylineChart.getXYPlot();
		LogAxis logAxis = new LogAxis("Logarithm USDT");
		logAxis.setMinorTickMarksVisible(true);
		logAxis.setAutoRange(true);
		xyplot.setRangeAxis(logAxis);
//		plot.setDomainAxis(new DateAxis());
		BufferedImage image = xylineChart.createBufferedImage(1080, 1200);
		ChartUtils.writeBufferedImageAsPNG(output, image);
	}

}
