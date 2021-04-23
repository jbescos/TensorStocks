package es.tododev.stocks;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class Main {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM");
	
	public static void main(String[] args) throws IOException, ParseException {
		File csv = new File("C:\\Users\\jorge\\Downloads\\bitstampUSD_1-min_data_2012-01-01_to_2021-03-31.csv");
		List<Pair<Number, String>> data = CsvParser.getRows(csv, DATE_FORMAT.parse("2021-03-01"), new Date(), "yyyy-MM-dd");
		Chart chart = new Chart();
		chart.add("Real", data);
		chart.save("target/histogram.png", "Bitcoin prediction", "Date", "Value");
	}
	
	private static void statsExample() throws IOException {
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
