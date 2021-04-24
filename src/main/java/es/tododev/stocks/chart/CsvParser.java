package es.tododev.stocks.chart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class CsvParser {

	public static List<CsvRow> getRows(File csv, boolean skipFirstLine, int limit, Function<String, CsvRow> parser) throws IOException {
		List<CsvRow> rows = new ArrayList<>();
		int i = 0;
		try (FileReader fr = new FileReader(csv); BufferedReader br = new BufferedReader(fr)) {
			String line;  
			while((line=br.readLine()) != null) {  
				if (i < limit) {
					if (skipFirstLine) {
						skipFirstLine = false;
					} else {
						CsvRow row = parser.apply(line);
						if (row != null) {
							i++;
							rows.add(row);
						}
					}
				} else {
					break;
				}
			}
		}
		return rows;
	}
	
	public static List<CsvRow> getRows(File csv, Date from, Date to) throws IOException {
		List<CsvRow> data = getRows(csv, true, Integer.MAX_VALUE, line -> {
			String[] columns = line.split(",");
			String timestamp = columns[0];
			String value = columns[1];
			Date date = new Date(Long.parseLong(timestamp) * 1000);
			if (date.getTime() >= from.getTime() && date.getTime() <= to.getTime()) {
				CsvRow row = new CsvRow(date, Double.parseDouble(value));
				return row;
			} else {
				return null;
			}
		});
		return data;
	}
	
}
