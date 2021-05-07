package com.jbescos.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jbescos.common.CsvUtil;

public class CsvUtilTest {

	@Test
	public void csv() throws IOException, SQLException {
		Path path = Files.createTempFile("test", ".csv");
		File file = path.toFile();
		System.out.println("CSV in " + file.getAbsolutePath());
		Map<String, Object> entry = new HashMap<>();
		entry.put("k1", "v1");
		entry.put("k2", "v2");
		CsvUtil.writeCsv(Arrays.asList(entry), ',', new FileOutputStream(file));
	}

}
