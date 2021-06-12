package com.jbescos.cloudchart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.IRow;

public class BarChart implements IChart<IRow> {
	
	private static final Logger LOGGER = Logger.getLogger(BarChart.class.getName());
	private final Map<String, CsvRow> current;
	private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	
	public BarChart(Map<String, CsvRow> current) {
		this.current = current;
	}

	@Override
	public void add(String lineLabel, List<? extends IRow> data) {
		if (!data.isEmpty()) {
			List<CsvTransactionRow> transactions = (List<CsvTransactionRow>) data;
			CsvTransactionRow first = transactions.get(0);
			CsvRow row = current.get(first.getSymbol());
			dataset.addValue(row.getPrice(), "CURRENT_VALUE", row.getSymbol());
			LOGGER.info("History of transactions: " + transactions);
			String symbol = first.getSymbol();
			dataset.addValue(avg(transactions), "AVG_" + first.getLabel(), symbol);
		}
	}
	
	private double avg(List<CsvTransactionRow> entries) {
		if (entries != null && !entries.isEmpty()) {
			double totalPrice = 0;
			int totalItems = 0;
			for (CsvTransactionRow row : entries) {
				totalPrice = totalPrice + row.getUsdtUnit();
				totalItems++;
			}
			return totalPrice / totalItems;
		}
		return 0;
	}

	@Override
	public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel)
			throws IOException {
		JFreeChart barChart = ChartFactory.createBarChart("Minimum profitable prices", "Symbols", "USDT", dataset,
				PlotOrientation.VERTICAL, true, true, false);
		barChart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
		BufferedImage image = barChart.createBufferedImage(1080, 1200);
		ChartUtils.writeBufferedImageAsPNG(output, image);
	}

}
