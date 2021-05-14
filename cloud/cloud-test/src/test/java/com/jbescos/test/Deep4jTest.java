package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.junit.Test;

import com.jbescos.common.CsvContent;
import com.jbescos.common.CsvUtil;

public class Deep4jTest {

	@Test
	public void transform() throws IOException {
		CsvContent content = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Deep4jTest.class.getResourceAsStream("/small.csv")))) {
			content = CsvUtil.readCsvContent(true, reader);
		}
		// DATE,SYMBOL,PRICE
		Schema initialSchema = new Schema.Builder()
				.addColumnLong("DATE")
				.addColumnCategorical("SYMBOL", new ArrayList<>(content.getCategories()))
				.addColumnDouble("PRICE").build();
		TransformProcess transformProcess = new TransformProcess.Builder(initialSchema)
				.categoricalToInteger("SYMBOL")
				.convertToInteger("DATE")
				.convertToDouble("PRICE")
				.build();
		transformProcess.getActionList().stream().forEach(action -> System.out.println(action));
		List<List<Writable>> output = new ArrayList<>(content.getParsedLines().size());
		for (String line : content.getParsedLines()) {
			List<String> column = Arrays.asList(line.split(","));
			assertEquals(column.size(), initialSchema.numColumns());
			System.out.println(column);
			List<Writable> input = transformProcess.transformRawStringsToInputList(column);
			output.add(transformProcess.execute(input));
		}
		
		System.out.println(output);
	}
}
