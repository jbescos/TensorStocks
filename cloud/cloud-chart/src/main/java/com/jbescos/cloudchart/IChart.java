package com.jbescos.cloudchart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Supplier;

public interface IChart<T> {

	void add(String lineLabel, List<Supplier<T>> data);
	void save(OutputStream output, String title, String horizontalLabel, String verticalLabel) throws IOException;
}
