package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.jbescos.common.CsvUtil;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.Utils;
import com.jbescos.test.util.TestFileStorage;

public class CsvUtilTest {

	private final String BASE_TEST_FOLDER = "./target/";

	@Test
	public void csv() throws IOException {
		Path path = Files.createTempFile("test", ".csv");
		File file = path.toFile();
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
	public void profitableCsv() throws IOException {
		TestFileStorage storage = new TestFileStorage(BASE_TEST_FOLDER, Collections.emptyList(), Collections.emptyList());
		CsvProfitRow row1 = new CsvProfitRow(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-01 00:46:01"), Utils.fromString(Utils.FORMAT_SECOND, "2021-05-04 00:46:01"), "symbol1", "1", "2", "3", "4", "10%", "11", "21", "16", "17%");
		CsvProfitRow row2 = new CsvProfitRow(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-02 00:46:01"), Utils.fromString(Utils.FORMAT_SECOND, "2021-05-03 00:46:01"), "symbol2", "1.1", "2.1", "3.1", "4.1", "10.1%", "11.1", "21.1", "16.1", "17.1%");
		StringBuilder builder = new StringBuilder().append(row1.toCsvLine()).append(row2.toCsvLine());
		storage.updateFile("profitable.csv", builder.toString().getBytes(), CsvProfitRow.HEADER.getBytes());
		List<CsvProfitRow> profits = storage.loadCsvProfitRows("userId", -1);
		assertEquals(row1, profits.get(0));
		assertEquals(row2, profits.get(1));
		String result = Utils.profitSummary(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-05 00:00:00"), 30, profits);
		assertEquals("Last 30 days: 4.1$-8.1$, 4$(97.56%) ✅", result);
	}
}
