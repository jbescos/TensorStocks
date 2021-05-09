package com.jbescos.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;

public class CsvUtil {

	private static final Logger LOGGER = Logger.getLogger(CsvUtil.class.getName());

	public static void writeCsv(List<Map<String, Object>> csv, char separator, OutputStream output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
		for (Map<String, Object> row : csv) {
			StringBuilder builder = new StringBuilder();
			for (Entry<String, Object> entry : row.entrySet()) {
				if (builder.length() != 0) {
					builder.append(separator);
				}
				Object value = entry.getValue();
				if (value instanceof Date) {
					builder.append(((Date)value).getTime()/1000);
				} else {
					builder.append(value.toString());
				}
			}
			writer.append(builder.toString());
			writer.newLine();
			writer.flush();
		}
	}

	public static <T> List<T> readCsv(boolean skipFirst, String separator, Function<String[], T> mapper, InputStream input) throws IOException {
		List<T> content = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
			while(reader.ready()) {
			     String line = reader.readLine();
			     if (skipFirst) {
			    	 skipFirst = false;
			     } else {
			    	 String[] columns = line.split(",");
			    	 content.add(mapper.apply(columns));
			     }
			}
		}
		LOGGER.info("Rows read:  " + content.size());
		return content;
	}
}
