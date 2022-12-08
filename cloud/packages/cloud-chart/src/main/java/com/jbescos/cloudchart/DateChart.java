package com.jbescos.cloudchart;

import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.jbescos.exchange.IRow;
import com.jbescos.exchange.Utils;

public class DateChart implements IChart<IRow> {

    private final TimeSeriesCollection dataset = new TimeSeriesCollection();

    @Override
    public void add(String lineLabel, List<? extends IRow> data) {
        TimeSeries series = new TimeSeries(lineLabel);
        TimeSeries seriesAvg = null;
        TimeSeries seriesAvg2 = null;
        boolean avg = false;
        boolean avg2 = false;
        if (!data.isEmpty()) {
            avg = data.get(0).getAvg() != null;
            if (avg) {
                seriesAvg = new TimeSeries(lineLabel + "-AVG");
            }
            avg2 = data.get(0).getAvg() != null;
            if (avg2) {
                seriesAvg2 = new TimeSeries(lineLabel + "-AVG_2");
            }
        }
        Map<String, List<IRow>> grouped = data.stream()
                .collect(Collectors.groupingBy(row -> Utils.fromDate(Utils.FORMAT, row.getDate())));
        DateFormat format = new SimpleDateFormat(Utils.FORMAT);
        for (String dateStr : grouped.keySet()) {
            List<IRow> inDate = grouped.get(dateStr);
            IRow last = inDate.get(inDate.size() - 1);
            Date date = Utils.fromString(format, dateStr);
            series.add(new Day(date), last.getPrice());
            if (avg) {
                seriesAvg.add(new Day(date), last.getAvg());
            }
            if (avg2) {
                seriesAvg2.add(new Day(date), last.getAvg2());
            }
        }
        dataset.addSeries(series);
        if (avg) {
            dataset.addSeries(seriesAvg);
        }
        if (avg2) {
            dataset.addSeries(seriesAvg2);
        }
    }

    @Override
    public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel)
            throws IOException {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, horizontalLabel, verticalLabel, dataset, true,
                true, true);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesStroke(0, new BasicStroke(0.2f));
        chart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
        ((XYPlot) chart.getPlot()).setRenderer(renderer);
        ((XYPlot) chart.getPlot()).setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);
        BufferedImage image = chart.createBufferedImage(1920, 1080);
        ChartUtils.writeBufferedImageAsPNG(output, image);
    }

}
