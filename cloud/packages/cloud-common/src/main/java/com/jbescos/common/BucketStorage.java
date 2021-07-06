package com.jbescos.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Acl.User;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.ComposeRequest;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;

public class BucketStorage {
	
	private static final Logger LOGGER = Logger.getLogger(BucketStorage.class.getName());
	
	public static List<CsvRow> withAvg(Date now, List<Price> prices) throws IOException{
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		Map<String, CsvRow> previousRows = new LinkedHashMap<>();
		Blob retrieve = storage.get(CloudProperties.BUCKET, Utils.LAST_PRICE);
		StringBuilder builder = new StringBuilder(Utils.CSV_ROW_HEADER);
		if (retrieve == null) {
			LOGGER.warning(Utils.CSV_ROW_HEADER + " was not found");
			// TODO It is better to get it from the last records of the date.csv
		} else {
			try (ReadChannel readChannel = retrieve.reader();
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				List<CsvRow> csv = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
				for (CsvRow row : csv) {
					previousRows.put(row.getSymbol(), row);
				}
			}
		}
		List<CsvRow> newRows = new ArrayList<>();
		for (Price price : prices) {
			CsvRow previous = previousRows.get(price.getSymbol());
			CsvRow newRow = null;
			if (previous != null) {
				newRow = new CsvRow(now, price.getSymbol(), price.getPrice(), Utils.ewma(CloudProperties.EWMA_CONSTANT, price.getPrice(), previous.getAvg()), Utils.ewma(CloudProperties.EWMA_2_CONSTANT, price.getPrice(), previous.getAvg2()));
			} else {
				newRow = new CsvRow(now, price.getSymbol(), price.getPrice(), price.getPrice(), price.getPrice());
			}
			newRows.add(newRow);
			builder.append(Utils.fromDate(Utils.FORMAT_SECOND, now)).append(",").append(newRow.getSymbol()).append(",").append(newRow.getPrice()).append(",").append(newRow.getAvg()).append(",").append(newRow.getAvg2()).append("\r\n");
		}
		storage.create(createBlobInfo(Utils.LAST_PRICE, false), builder.toString().getBytes(Utils.UTF8));
		return newRows;
	}
	
	public static String updateFile(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		BlobInfo retrieve = storage.get(BlobInfo.newBuilder(CloudProperties.BUCKET, fileName).build().getBlobId());
		if (retrieve == null) {
			retrieve = storage.create(createBlobInfo(fileName, false), header);
		}
		final String TEMP_FILE = fileName + ".tmp";
		storage.create(createBlobInfo(TEMP_FILE, false), content);
		BlobInfo blobInfo = createBlobInfo(fileName, false);
		ComposeRequest request = ComposeRequest.newBuilder().setTarget(blobInfo)
				.addSource(fileName).addSource(TEMP_FILE).build();
		blobInfo = storage.compose(request);
		storage.delete(CloudProperties.BUCKET, TEMP_FILE);
		return blobInfo.getMediaLink();
	}
	
	public static String fileToString(String fileName) throws IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		StringBuilder builder = new StringBuilder();
		try (ReadChannel readChannel = storage.reader(CloudProperties.BUCKET, fileName); BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));){
			List<String> lines = CsvUtil.readLines(false, reader);
			for (String line : lines) {
				builder.append(line);
			}
		}
		return builder.toString();
	}

	private static void updateTotalCsv(Storage storage, byte[] header, String newDataFile, String totalCsv) {
		BlobInfo total = storage.get(BlobInfo.newBuilder(CloudProperties.BUCKET, totalCsv).build().getBlobId());
		if (total == null) {
			total = storage.create(createBlobInfo(totalCsv, false), header);
		}
		ComposeRequest request = ComposeRequest.newBuilder().setTarget(total).addSource(totalCsv).addSource(newDataFile).build();
		storage.compose(request);
	}

	private static BlobInfo createBlobInfo(String fileName, boolean acl) {
		BlobInfo.Builder builder = BlobInfo.newBuilder(CloudProperties.BUCKET, fileName);
		if (acl) {
			builder.setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.READER))));
		}
		builder.setContentType("text/csv").setContentEncoding(Utils.UTF8.name()).setStorageClass(StorageClass.STANDARD);
		return builder.build();
	}

}
