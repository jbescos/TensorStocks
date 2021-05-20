package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class DateChart implements IChart<IRow> {

	private final TimeSeriesCollection dataset = new TimeSeriesCollection();

	@Override
	public void add(String lineLabel, List<? extends IRow> data) {
		TimeSeries series = new TimeSeries(lineLabel);
		Map<String, List<IRow>> grouped = data.stream().collect(Collectors.groupingBy(row -> Utils.fromDate(Utils.FORMAT, row.getDate())));
		for (String date : grouped.keySet()) {
			IRow row = grouped.get(date).get(0);
			series.add(new Day(Utils.fromString(Utils.FORMAT, date)), row.getPrice());
		}
		dataset.addSeries(series);
	}

	@Override
	public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel)
			throws IOException {
		JFreeChart chart = ChartFactory.createTimeSeriesChart(title, horizontalLabel, verticalLabel, dataset, true,
				true, true);
		BufferedImage image = chart.createBufferedImage(1080, 1200);
		ChartUtils.writeBufferedImageAsPNG(output, image);
	}

}
