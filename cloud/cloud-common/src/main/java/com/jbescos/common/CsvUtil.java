package com.jbescos.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CsvUtil {
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
}
