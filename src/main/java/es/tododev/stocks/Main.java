package es.tododev.stocks;

import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class Main {

	public static void main(String[] args) throws IOException {
		
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		dataset.addValue(212, "Real", "2021-01-01");
		dataset.addValue(504, "Real", "2021-02-01");
		dataset.addValue(1520, "Real", "2021-03-01");
		dataset.addValue(1842, "Real", "2021-04-01");
		dataset.addValue(2991, "Real", "2021-05-01");
		dataset.addValue(324, "Prediction", "2021-01-01");
		dataset.addValue(5677, "Prediction", "2021-02-01");
		dataset.addValue(54, "Prediction", "2021-03-01");
		dataset.addValue(709, "Prediction", "2021-04-01");
		dataset.addValue(2488, "Prediction", "2021-05-01");

		JFreeChart histogram = ChartFactory.createLineChart("Bitcoin prediction", "Date", "Value",
				dataset, PlotOrientation.VERTICAL, true, true, true);

		ChartUtils.saveChartAsPNG(new File("target/histogram.png"), histogram, 1080, 1200);

	}

}
