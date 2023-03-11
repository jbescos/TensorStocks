package com.jbescos.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;

import com.jbescos.exchange.Kline;

public class CandleChart implements IChart<Kline> {

    private final OHLCSeriesCollection candlestickDataset = new OHLCSeriesCollection();

    private final Map<String, Object> properties;
    
    public CandleChart() {
        this.properties = defaultProperties();
    }

    @Override
    public void add(String lineLabel, List<? extends Kline> data) {
        OHLCSeries item = new OHLCSeries(lineLabel);
        for (Kline kline : data) {
            item.add(new Day(new Date(kline.getOpenTime())), Double.parseDouble(kline.getOpen()),
                    Double.parseDouble(kline.getHigh()), Double.parseDouble(kline.getLow()),
                    Double.parseDouble(kline.getClose()));
        }
        candlestickDataset.addSeries(item);
    }

    @Override
    public void save(OutputStream output)
            throws IOException {
        JFreeChart chart = ChartFactory.createCandlestickChart((String) properties.get(TITLE), (String) properties.get(HORIZONTAL_LABEL), (String) properties.get(VERTICAL_LABEL),
                candlestickDataset, true);
        chart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
        BufferedImage image = chart.createBufferedImage((int) properties.get(WIDTH), (int) properties.get(HEIGTH));
        ChartUtils.writeBufferedImageAsPNG(output, image);
    }

    @Override
    public void property(String key, Object value) {
        properties.put(key, value);
    }
}
