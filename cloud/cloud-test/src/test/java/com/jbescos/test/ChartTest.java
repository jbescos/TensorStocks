package com.jbescos.test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.jbescos.cloudchart.ChartGenerator;
import com.jbescos.cloudchart.XYChart;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;

public class ChartTest {

	private static final List<String> SYMBOLS = Arrays.asList("DOGEUSDT", "BTTUSDT", "ADAUSDT", "XRPUSDT", "MATICUSDT", "CHZUSDT", "GRTUSDT", "ANKRUSDT", "ADAUSDT", "SHIBUSDT");
	
	@Test
	public void generateChart() throws IOException {
		List<? extends IRow> rows = null;
		try (InputStream input = ChartTest.class.getResourceAsStream("/total.csv");
				InputStreamReader inputReader = new InputStreamReader(input);
				BufferedReader reader = new BufferedReader(inputReader);) {
			rows = CsvUtil.readCsvRows(true, ",", reader);
		}
		rows = rows.stream().filter(row -> SYMBOLS.contains(row.getSymbol())).collect(Collectors.toList());
		try (FileOutputStream output = new FileOutputStream("./target/chart.png")) {
			ChartGenerator.writeChart(rows, output, new XYChart());
		}
		
	}

}
