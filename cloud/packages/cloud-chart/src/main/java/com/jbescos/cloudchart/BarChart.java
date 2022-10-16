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

import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.IRow;
import com.jbescos.exchange.Broker.Action;

public class BarChart implements IChart<IRow> {
	
	private static final Logger LOGGER = Logger.getLogger(BarChart.class.getName());
	private final Map<String,Double> wallet;
	private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	
	public BarChart(Map<String,Double> wallet) {
		this.wallet = wallet;
	}

	@Override
	public void add(String lineLabel, List<? extends IRow> data) {
		if (!data.isEmpty()) {
			List<CsvTransactionRow> transactions = (List<CsvTransactionRow>) data;
			CsvTransactionRow first = transactions.get(0);
			LOGGER.info(() -> "History of transactions: " + transactions);
			String symbol = first.getSymbol();
			double sum = sum(transactions);
			Double walletPrice = wallet.get(symbol);
			if (walletPrice == null) {
				walletPrice = 0.0;
			}
			String label = first.getLabel();
			label = label.replaceAll(Action.SELL_PANIC.name(), Action.SELL.name());
			if (first.getSide() == Action.BUY) {
                dataset.addValue(sum, "SUM_" + label, symbol);
            } else {
                dataset.addValue(sum, "SUM_" + label, symbol);
                dataset.addValue(walletPrice + sum, "SUM_WALLET_AND_" + label, symbol);
            }
			dataset.addValue(walletPrice, "PENDING_WALLET", symbol);
		}
	}
	
	private double sum(List<CsvTransactionRow> entries) {
		if (entries != null && !entries.isEmpty()) {
			double totalPrice = 0;
			for (CsvTransactionRow row : entries) {
				totalPrice = totalPrice + Double.parseDouble(row.getUsdt());
			}
			return totalPrice;
		}
		return 0;
	}

	@Override
	public void save(OutputStream output, String title, String horizontalLabel, String verticalLabel)
			throws IOException {
		JFreeChart barChart = ChartFactory.createBarChart("Total buy/sell", "Symbols", "USDT", dataset,
				PlotOrientation.VERTICAL, true, true, false);
		barChart.getPlot().setBackgroundPaint(IChart.BACKGROUND_COLOR);
		BufferedImage image = barChart.createBufferedImage(1920, 1080);
		ChartUtils.writeBufferedImageAsPNG(output, image);
	}

}
