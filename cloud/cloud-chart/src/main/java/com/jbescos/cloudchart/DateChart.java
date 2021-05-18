package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
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

import com.jbescos.common.CsvAccountRow;

public class DateChart implements IChart<CsvAccountRow> {

	private final TimeSeriesCollection dataset = new TimeSeriesCollection();

	@Override
	public void add(String lineLabel, List<CsvAccountRow> data) {
		TimeSeries series = new TimeSeries(lineLabel);
		Map<Date, List<CsvAccountRow>> grouped = data.stream().collect(Collectors.groupingBy(CsvAccountRow::getDate));
		List<Date> dates = new ArrayList<>(grouped.keySet());
		Collections.sort(dates);
		for (Date date : dates) {
			CsvAccountRow row = grouped.get(date).get(0);
			series.add(new Day(date), row.getUsdt());
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
