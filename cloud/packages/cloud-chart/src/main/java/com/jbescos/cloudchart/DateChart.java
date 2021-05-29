package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
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
		TimeSeries seriesAvg = null;
		boolean avg = false;
		if (! data.isEmpty()) {
			avg = data.get(0).getAvg() != null;
			if (avg) {
				seriesAvg = new TimeSeries(lineLabel + "-AVG");
			}
		}
		Map<String, List<IRow>> grouped = data.stream().collect(Collectors.groupingBy(row -> Utils.fromDate(Utils.FORMAT, row.getDate())));
		for (String dateStr : grouped.keySet()) {
			List<IRow> inDate = grouped.get(dateStr);
			IRow last = inDate.get(inDate.size() - 1);
			Date date = Utils.fromString(Utils.FORMAT, dateStr);
			series.add(new Day(date), last.getPrice());
			if (avg) {
				seriesAvg.add(new Day(date), last.getAvg());
			}
		}
		dataset.addSeries(series);
		if (avg) {
			dataset.addSeries(seriesAvg);
		}
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
