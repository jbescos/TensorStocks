package com.jbescos.cloudchart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.IRow;
import com.jbescos.common.Utils;

public class ChartGenerator {

	private static final Logger LOGGER = Logger.getLogger(ChartGenerator.class.getName());
	
	public static void writeLoadAndWriteChart(OutputStream output, int daysBack) throws IOException {
		String filePrefix = "account_";
		List<String> days = Utils.daysBack(new Date(), daysBack, filePrefix, ".csv");
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		IChart<IRow> chart = create();
		List<IRow> rows = new ArrayList<>();
		Page<Blob> blobs = storage.list(CloudProperties.BUCKET, BlobListOption.prefix(filePrefix));
		for (Blob blob : blobs.iterateAll()) {
			String fileName = blob.getName();
			if (days.contains(fileName)) {
				try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, fileName);
						BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
					List<? extends IRow> csv = CsvUtil.readCsvAccountRows(true, ",", reader);
					rows.addAll(csv);
				}
			}
		}
		writeChart(rows, output, chart);
	}

	private static void writeChart(List<? extends IRow> rows, OutputStream output, IChart<IRow> chart)
			throws IOException {
		Map<String, List<IRow>> grouped = rows.stream().collect(Collectors.groupingBy(IRow::getSymbol));
		for (Entry<String, List<IRow>> entry : grouped.entrySet()) {
			chart.add(entry.getKey(), entry.getValue());
		}
		chart.save(output, "Crypto currencies", "", "USDT");
	}

	private static IChart<IRow> create() {
		if ("date".equals(CloudProperties.CHART_TYPE)) {
			return new DateChart();
		} else {
			return new XYChart();
		}
	}

}
