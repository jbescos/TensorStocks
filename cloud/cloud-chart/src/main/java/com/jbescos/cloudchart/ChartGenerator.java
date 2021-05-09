package com.jbescos.cloudchart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Utils;

public class ChartGenerator {

	private static final String BUCKET;
	private static final String PROJECT_ID;
	private static final String TOTAL_FILE = "total.csv";

	static {
		try {
			Properties properties = Utils.fromClasspath("/storage.properties");
			BUCKET = properties.getProperty("storage.bucket");
			PROJECT_ID = properties.getProperty("project.id");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void writeChart(OutputStream output) throws IOException {
		List<CsvRow> csv = null;
		Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
		// FIXME does not work
		try (ReadChannel reader = storage.reader(BUCKET, TOTAL_FILE)) {
			InputStream input = Channels.newInputStream(reader);
			csv = CsvUtil.readCsv(true, ",", columns -> new CsvRow(Utils.fromString(Utils.FORMAT_SECOND, columns[0]), columns[1].replaceFirst("USDT", ""), Double.parseDouble(columns[2])), input);
		}
		Map<String, List<CsvRow>> grouped = csv.stream().collect(Collectors.groupingBy(CsvRow::getSymbol));
		csv = null;
		IChart chart = new XYChart();
		for (Entry<String, List<CsvRow>> entry : grouped.entrySet()) {
			chart.add(entry.getKey(), entry.getValue());
		}
		chart.save(output, "Crypto currencies", "", "USDT");
	}

}
