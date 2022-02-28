package com.jbescos.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Acl.User;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.ComposeRequest;
import com.google.cloud.storage.StorageClass;

public class BucketStorage implements FileManager {
	
	private static final Logger LOGGER = Logger.getLogger(BucketStorage.class.getName());
	private final Storage storage;
	private final CloudProperties cloudProperties;
	
	public BucketStorage(CloudProperties cloudProperties, Storage storage) {
		this.cloudProperties = cloudProperties;
	    this.storage = storage;
	}
	
	public Map<String, CsvRow> previousRows(long serverTime, String lastUpdated) throws IOException {
		Map<String, CsvRow> previousRows = new LinkedHashMap<>();
		Blob retrieve = storage.get(cloudProperties.BUCKET, lastUpdated);
		if (retrieve == null) {
			LOGGER.warning(lastUpdated + " was not found");
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
		return previousRows;
	}

	public List<CsvRow> updatedRowsAndSaveLastPrices(Map<String, CsvRow> previousRows, Map<String, Double> prices, Date now, String lastPriceCsv, int fearGreedIndex) {
	    StringBuilder builder = new StringBuilder(Utils.CSV_ROW_HEADER);
	    List<CsvRow> newRows = new ArrayList<>();
        for (Entry<String, Double> price : prices.entrySet()) {
            CsvRow previous = previousRows.get(price.getKey());
            CsvRow newRow = null;
            if (previous != null) {
                newRow = new CsvRow(now, price.getKey(), price.getValue(), Utils.ewma(cloudProperties.EWMA_CONSTANT, price.getValue(), previous.getAvg()), Utils.ewma(cloudProperties.EWMA_2_CONSTANT, price.getValue(), previous.getAvg2()), fearGreedIndex);
            } else {
                newRow = new CsvRow(now, price.getKey(), price.getValue(), price.getValue(), price.getValue(), fearGreedIndex);
            }
            newRows.add(newRow);
            builder.append(newRow.toCsvLine());
        }
        storage.create(createBlobInfo(cloudProperties, lastPriceCsv, false), builder.toString().getBytes(Utils.UTF8));
        return newRows;
	}

	@Override
	public String updateFile(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		BlobInfo retrieve = storage.get(BlobInfo.newBuilder(cloudProperties.BUCKET, fileName).build().getBlobId());
		if (retrieve == null) {
			retrieve = storage.create(createBlobInfo(cloudProperties, fileName, false), header);
		}
		final String TEMP_FILE = cloudProperties.USER_ID + "/" + fileName + ".tmp";
		storage.create(createBlobInfo(cloudProperties, TEMP_FILE, false), content);
		BlobInfo blobInfo = createBlobInfo(cloudProperties, fileName, false);
		ComposeRequest request = ComposeRequest.newBuilder().setTarget(blobInfo)
				.addSource(fileName).addSource(TEMP_FILE).build();
		blobInfo = storage.compose(request);
		storage.delete(cloudProperties.BUCKET, TEMP_FILE);
		return blobInfo.getMediaLink();
	}

	private static BlobInfo createBlobInfo(CloudProperties cloudProperties, String fileName, boolean acl) {
		BlobInfo.Builder builder = BlobInfo.newBuilder(cloudProperties.BUCKET, fileName);
		if (acl) {
			builder.setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.READER))));
		}
		builder.setContentType("text/csv").setContentEncoding(Utils.UTF8.name()).setStorageClass(StorageClass.STANDARD);
		return builder.build();
	}
	
	public Page<Blob> list(String bucket) {
		return storage.list(bucket, BlobListOption.currentDirectory());
	}

	@Override
	public List<CsvTransactionRow> loadTransactions() throws IOException {
		List<CsvTransactionRow> transactions = new ArrayList<>();
		List<String> months = Utils.monthsBack(new Date(), cloudProperties.BOT_MONTHS_BACK_TRANSACTIONS, cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
		Page<Blob> transactionFiles = storage.list(cloudProperties.BUCKET, BlobListOption.prefix(cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX));
		for (Blob transactionFile : transactionFiles.iterateAll()) {
		    if (months.contains(transactionFile.getName())) {
		        try (ReadChannel readChannel = transactionFile.reader();
                        BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
                    List<CsvTransactionRow> csv = CsvUtil.readCsvTransactionRows(true, ",", reader);
                    transactions.addAll(csv);
                }
		    }
		}
		return transactions;
	}

	@Override
	public List<CsvRow> loadPreviousRows() throws IOException {
		// Get 1 day more and compare dates later
		List<String> days = Utils.daysBack(new Date(), (cloudProperties.BOT_HOURS_BACK_STATISTICS / 24) + 1, "data" + cloudProperties.USER_EXCHANGE.getFolder(), ".csv");
		List<CsvRow> rows = new ArrayList<>();
		Date now = new Date();
		Date from = Utils.getDateOfHoursBack(now, cloudProperties.BOT_HOURS_BACK_STATISTICS);
		List<CsvRow> csvInDay = null;
		for (String day : days) {
			try (ReadChannel readChannel = storage.reader(cloudProperties.BUCKET, day);
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				csvInDay = CsvUtil.readCsvRows(true, ",", reader, cloudProperties.BOT_WHITE_LIST_SYMBOLS);
				csvInDay = csvInDay.stream().filter(row -> row.getDate().getTime() > from.getTime())
						.collect(Collectors.toList());
				rows.addAll(csvInDay);
			}
		}
		return rows;
	}

	@Override
	public String overwriteFile(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(content);
		BlobInfo blobInfo = storage.create(createBlobInfo(cloudProperties, fileName, false), outputStream.toByteArray());
		return blobInfo.getMediaLink();
	}

}
