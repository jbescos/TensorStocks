package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Test;

import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class CsvUtilTest {

	private static final Logger LOGGER = Logger.getLogger(CsvUtilTest.class.getName());

	@Test
	public void csv() throws IOException, SQLException {
		Path path = Files.createTempFile("test", ".csv");
		File file = path.toFile();
		System.out.println("CSV in " + file.getAbsolutePath());
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("k1", "v1");
		entry.put("k2", "v2");
		try (OutputStream output = new FileOutputStream(file)) {
			CsvUtil.writeCsv(Arrays.asList(entry, entry), ',', output);
		}
		List<Map<String, String>> read = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			read = CsvUtil.readCsv(true, line -> {
				String columns[] = line.split(",");
				Map<String, String> row = new HashMap<>();
				row.put("k1", columns[0]);
				row.put("k2", columns[1]);
				return row;
			}, reader);
		}
		assertEquals(1, read.size());
		assertEquals("v1", read.get(0).get("k1"));
		assertEquals("v2", read.get(0).get("k2"));
	}

	@Test
	public void sellMaxBuyMin() throws IOException {
		List<? extends IRow> rows = null;
		try (InputStream input = ChartTest.class.getResourceAsStream("/total.csv");
				InputStreamReader inputReader = new InputStreamReader(input);
				BufferedReader reader = new BufferedReader(inputReader);) {
			rows = CsvUtil.readCsvRows(true, ",", reader);
		}
		Map<String, List<IRow>> grouped = rows.stream().filter(price -> price.getSymbol().endsWith("USDT"))
				.filter(price -> !price.getSymbol().endsWith("UPUSDT"))
				.filter(price -> !price.getSymbol().endsWith("DOWNUSDT")).collect(Collectors.groupingBy(IRow::getSymbol));
		for (Entry<String, List<IRow>> entry : grouped.entrySet()) {
			buyAndSell(entry.getKey(), entry.getValue());
		}
	}
	
	private void buyAndSell(String symbol, List<IRow> data) {
		List<MinMax> minMax = calculateMinMax(data);
		if (!minMax.isEmpty()) {
			double usdt = 1;
			double amountSymbol = 0;
			for (MinMax m : minMax) {
				if (m.type == MinMaxEnum.MAX) {
					usdt = m.row.getPrice() * amountSymbol;
					amountSymbol = 0;
				} else {
					amountSymbol = usdt / m.row.getPrice();
					usdt = 0;
				}
			}
			LOGGER.info(Utils.format(usdt) + "$, " + Utils.format(amountSymbol) + symbol);
		}
	}

	private List<MinMax> calculateMinMax(List<IRow> data) {
		List<MinMax> minMax = new ArrayList<>();
		IRow previous = data.get(0);
		IRow middle = data.get(1);
		for (int i = 2; i < data.size(); i++) {
			IRow current = data.get(i);
			if (middle.getPrice() > previous.getPrice() && middle.getPrice() > current.getPrice()) {
				// Max
				minMax.add(new MinMax(MinMaxEnum.MAX, middle));
			} else if (middle.getPrice() < previous.getPrice() && middle.getPrice() < current.getPrice()) {
				// Min
				minMax.add(new MinMax(MinMaxEnum.MIN, middle));
			}
			previous = middle;
			middle = current;
		}
		LOGGER.info("Min and Max: " + minMax);
		return minMax;
	}

	private static class MinMax {

		private final MinMaxEnum type;
		private final IRow row;
		public MinMax(MinMaxEnum type, IRow row) {
			this.type = type;
			this.row = row;
		}
		@Override
		public String toString() {
			return type + " price=" + Utils.format(row.getPrice());
		}
	}
	
	private static enum MinMaxEnum {
		MIN, MAX;
	}

}
