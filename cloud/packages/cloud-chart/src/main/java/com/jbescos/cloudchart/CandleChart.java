package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;

import com.jbescos.exchange.Kline;

public class CandleChart implements IChart<Kline> {

	private final OHLCSeriesCollection candlestickDataset = new OHLCSeriesCollection();
	
	@Override
	public void add(String lineLabel, List<? extends Kline> data) {
		OHLCSeries item = new OHLCSeries(lineLabel);
		for (Kline kline : data) {
			item.add(new Day(new Date(kline.getOpenTime())), Double.parseDouble(kline.getOpen()), Double.parseDouble(kline.getHigh()), Double.parseDouble(kline.getLow()), Double.parseDouble(kline.getClose()));
		}
		candlestickDataset.addSeries(item);
	}

	@Override
	public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel)
			throws IOException {
		JFreeChart chart = ChartFactory.createCandlestickChart(title, horizontalLabel, verticalLabel, candlestickDataset, true);
		chart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
		BufferedImage image = chart.createBufferedImage(1080, 1200);
		ChartUtils.writeBufferedImageAsPNG(output, image);
	}

}
