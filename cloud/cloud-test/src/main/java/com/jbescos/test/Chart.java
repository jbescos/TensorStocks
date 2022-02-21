package com.jbescos.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import com.jbescos.cloudchart.ChartGenerator;
import com.jbescos.cloudchart.IChart;
import com.jbescos.cloudchart.XYChart;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class Chart {
	
	private static final Logger LOGGER = Logger.getLogger(Chart.class.getName());
	private static final Function<String, CsvRow> SELL_FUNCTION = createCsvMapper("SELL");
	private static final Function<String, CsvRow> BUY_FUNCTION = createCsvMapper("BUY");

	public static void main(String args[]) throws IOException {
		generate(new File("C:\\workspace\\TensorStocks\\cloud\\packages\\local-bot\\BTCUSDT.csv"));
	}
	
	private static void generate(File csv) throws FileNotFoundException, IOException {
		List<CsvRow> sell = null;
		List<CsvRow> buy = null;
		try (FileReader fr = new FileReader(csv); BufferedReader br = new BufferedReader(fr);) {
			sell = CsvUtil.readCsv(true,SELL_FUNCTION, br);
		}
		try (FileReader fr = new FileReader(csv); BufferedReader br = new BufferedReader(fr);) {
			buy = CsvUtil.readCsv(true,BUY_FUNCTION, br);
		}
		LOGGER.info("SELL: " + sell.size() + " rows, BUY: " + buy.size() + " rows");
		try (FileOutputStream output = new FileOutputStream(csv.getName() + ".png")) {
			IChart<IRow> chart = new XYChart();
			ChartGenerator.writeChart(sell, output, chart);
			ChartGenerator.writeChart(buy, output, chart);
			ChartGenerator.save(output, chart);
		}
	}
	
	private static Function<String, CsvRow> createCsvMapper(String direction){
	    DateFormat format = new SimpleDateFormat(Utils.FORMAT_SECOND);
		Function<String, CsvRow> function = line -> {
			String[] columns = line.split(",");
			String directionColumn = columns[3];
			if (direction.equals(directionColumn)) {
				Date date = Utils.fromString(format, columns[0]);
				String symbol = columns[1];
				double price = Double.parseDouble(columns[2]);
				double avg = Double.parseDouble(columns[4]);
				double longAvg = Double.parseDouble(columns[5]);
				CsvRow row = new CsvRow(date, direction + "_" + symbol, price, avg, longAvg, 50);
				return row;
			} else {
				return null;
			}
		};
		return function;
	}
}
