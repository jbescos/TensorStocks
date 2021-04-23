package es.tododev.stocks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class CsvParser {

	public static <N, M> List<Pair<N, M>> getRows(File csv, boolean skipFirstLine, int limit, Function<String, Pair<N, M>> parser) throws IOException {
		List<Pair<N, M>> rows = new ArrayList<>();
		int i = 0;
		try (FileReader fr = new FileReader(csv); BufferedReader br = new BufferedReader(fr)) {
			String line;  
			while((line=br.readLine()) != null) {  
				if (i < limit) {
					if (skipFirstLine) {
						skipFirstLine = false;
					} else {
						Pair<N, M> pair = parser.apply(line);
						if (pair != null) {
							i++;
							rows.add(pair);
						}
					}
				} else {
					break;
				}
			}
		}
		return rows;
	}
	
	public static List<Pair<Number, String>> getRows(File csv, Date from, Date to, String pattern) throws IOException {
		DateFormat DATE_FORMAT = new SimpleDateFormat(pattern);
		List<Pair<Number, String>> data = CsvParser.getRows(csv, true, Integer.MAX_VALUE, line -> {
			String[] columns = line.split(",");
			String timestamp = columns[0];
			String value = columns[7];
			if (!"NaN".equals(value)) {
				Date date = new Date(Long.parseLong(timestamp) * 1000);
				if (date.getTime() >= from.getTime() && date.getTime() <= to.getTime()) {
					Pair<Number, String> pair = new Pair<>(Double.parseDouble(value), DATE_FORMAT.format(date));
					return pair;
				}
			}
			return null;
		});
		return data;
	}
	
}
