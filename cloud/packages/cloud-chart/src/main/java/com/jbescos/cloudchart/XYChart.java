package com.jbescos.cloudchart;

import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.jbescos.common.IRow;

public class XYChart implements IChart<IRow> {

    private final XYSeriesCollection dataset = new XYSeriesCollection();

    @Override
    public void add(String lineLabel, List<? extends IRow> data) {
        XYSeries series = new XYSeries(lineLabel);
        XYSeries seriesAvg = null;
        XYSeries seriesAvg2 = null;
        boolean avg = false;
        boolean avg2 = false;
        if (!data.isEmpty()) {
            avg = data.get(0).getAvg() != null;
            if (avg) {
                seriesAvg = new XYSeries(lineLabel + "-AVG");
            }
            avg2 = data.get(0).getAvg() != null;
            if (avg2) {
                seriesAvg2 = new XYSeries(lineLabel + "-AVG_2");
            }
        }
        for (IRow row : data) {
            series.add(row.getDate().getTime(), row.getPrice());
            if (avg) {
                seriesAvg.add(row.getDate().getTime(), row.getAvg());
            }
            if (avg2) {
                seriesAvg2.add(row.getDate().getTime(), row.getAvg2());
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
        JFreeChart xylineChart = ChartFactory.createXYLineChart(title, horizontalLabel, verticalLabel, dataset,
                PlotOrientation.VERTICAL, true, true, true);
//	    XYPlot xyplot = xylineChart.getXYPlot();
//	    LogAxis logAxis = new LogAxis("Logarithm USDT");
//	    logAxis.setMinorTickMarksVisible(true);
//	    logAxis.setAutoRange(true);
//	    xyplot.setRangeAxis(logAxis);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        DateAxis axis = new DateAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("dd/MM hh:mm"));
        ((XYPlot) xylineChart.getPlot()).setDomainAxis(axis);
        ((XYPlot) xylineChart.getPlot()).setRenderer(renderer);
        ((XYPlot) xylineChart.getPlot()).setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);
        xylineChart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
        BufferedImage image = xylineChart.createBufferedImage(1080, 1200);
        ChartUtils.writeBufferedImageAsPNG(output, image);
    }

}
