package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.jbescos.common.CsvUtil;

public class CsvUtilTest {

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
		try (InputStream input = new FileInputStream(file)) {
			read = CsvUtil.readCsv(true, ",", columns -> {
				Map<String, String> row = new HashMap<>();
				row.put("k1", columns[0]);
				row.put("k2", columns[1]);
				return row;
			}, input);
		}
		assertEquals(1, read.size());
		assertEquals("v1", read.get(0).get("k1"));
		assertEquals("v2", read.get(0).get("k2"));
	}

}
