package com.jbescos.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.jbescos.exchange.IRow;

public class TxSummaryBarChart implements IChart<IRow> {

    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private final Map<String, Object> properties;

    public TxSummaryBarChart() {
        this.properties = defaultProperties();
    }
    
    @Override
    public void add(String lineLabel, List<? extends IRow> data) {
        if (!data.isEmpty()) {
            for (IRow row : data) {
                dataset.addValue(row.getPrice() * 100, row.getLabel(), row.getLabel());
            }
        }
    }

    @Override
    public void save(OutputStream output) throws IOException {
        JFreeChart barChart = ChartFactory.createBarChart("Open positions profit %", "Symbols", "Profit %", dataset,
                PlotOrientation.VERTICAL, true, true, false);
        barChart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
        BufferedImage image = barChart.createBufferedImage((int) properties.get(WIDTH), (int) properties.get(HEIGTH));
        ChartUtils.writeBufferedImageAsPNG(output, image);
    }

    @Override
    public void property(String key, Object value) {
        properties.put(key, value);
    }

}
