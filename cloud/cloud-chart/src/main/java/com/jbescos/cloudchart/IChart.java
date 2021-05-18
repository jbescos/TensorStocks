package com.jbescos.cloudchart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface IChart<T> {

	void add(String lineLabel, List<? extends T> data);
	void save(OutputStream output, String title, String horizontalLabel, String verticalLabel) throws IOException;
}
