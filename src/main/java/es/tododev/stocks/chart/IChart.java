package es.tododev.stocks.chart;

import java.io.IOException;
import java.util.List;

public interface IChart {

	void add(String lineLabel, List<CsvRow> data);
	void save(String filePath, String title, String horizontalLabel, String verticalLabel) throws IOException;
}
