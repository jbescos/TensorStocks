package com.jbescos.cloudchart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.jbescos.common.CsvRow;

public interface IChart {

	void add(String lineLabel, List<CsvRow> data);
	void save(OutputStream output, String title, String horizontalLabel, String verticalLabel) throws IOException;
}
