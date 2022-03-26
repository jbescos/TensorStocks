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
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.ComposeRequest;
import com.google.cloud.storage.StorageClass;
import com.jbescos.common.CloudProperties.Exchange;

public class BucketStorage implements FileManager {
	
	private static final Logger LOGGER = Logger.getLogger(BucketStorage.class.getName());
	private final StorageInfo storageInfo;
	
	public BucketStorage(StorageInfo storageInfo) {
		this.storageInfo = storageInfo;
	}
	
	public Map<String, CsvRow> previousRows(long serverTime, String lastUpdated) throws IOException {
		Map<String, CsvRow> previousRows = new LinkedHashMap<>();
		Blob retrieve = storageInfo.getStorage().get(storageInfo.getBucket(), lastUpdated);
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
                newRow = new CsvRow(now, price.getKey(), price.getValue(), Utils.ewma(Utils.EWMA_CONSTANT, price.getValue(), previous.getAvg()), Utils.ewma(Utils.EWMA_2_CONSTANT, price.getValue(), previous.getAvg2()), fearGreedIndex);
            } else {
                newRow = new CsvRow(now, price.getKey(), price.getValue(), price.getValue(), price.getValue(), fearGreedIndex);
            }
            newRows.add(newRow);
            builder.append(newRow.toCsvLine());
        }
        storageInfo.getStorage().create(createBlobInfo(storageInfo, lastPriceCsv, false), builder.toString().getBytes(Utils.UTF8));
        return newRows;
	}

	@Override
	public String updateFile(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		BlobInfo retrieve = storageInfo.getStorage().get(BlobInfo.newBuilder(storageInfo.getBucket(), fileName).build().getBlobId());
		if (retrieve == null) {
			retrieve = storageInfo.getStorage().create(createBlobInfo(storageInfo, fileName, false), header);
		}
		final String TEMP_FILE = fileName + ".tmp";
		storageInfo.getStorage().create(createBlobInfo(storageInfo, TEMP_FILE, false), content);
		BlobInfo blobInfo = createBlobInfo(storageInfo, fileName, false);
		ComposeRequest request = ComposeRequest.newBuilder().setTarget(blobInfo)
				.addSource(fileName).addSource(TEMP_FILE).build();
		blobInfo = storageInfo.getStorage().compose(request);
		storageInfo.getStorage().delete(storageInfo.getBucket(), TEMP_FILE);
		return blobInfo.getMediaLink();
	}

	private static BlobInfo createBlobInfo(StorageInfo storageInfo, String fileName, boolean acl) {
		BlobInfo.Builder builder = BlobInfo.newBuilder(storageInfo.getBucket(), fileName);
		if (acl) {
			builder.setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.READER))));
		}
		builder.setContentType("text/csv").setContentEncoding(Utils.UTF8.name()).setStorageClass(StorageClass.STANDARD);
		return builder.build();
	}
	
	public Page<Blob> list(String bucket) {
		return storageInfo.getStorage().list(bucket, BlobListOption.currentDirectory());
	}

	@Override
	public List<CsvTransactionRow> loadTransactions(String userId) throws IOException {
		Blob retrieve = storageInfo.getStorage().get(BlobInfo.newBuilder(storageInfo.getBucket(), userId + "/" + Utils.OPEN_POSSITIONS).build().getBlobId());
		List<CsvTransactionRow> transactions = null;
		if (retrieve == null) {
			LOGGER.warning(userId + "/" + Utils.OPEN_POSSITIONS + " was not found!. Reading all transactions of the last 12 months.");
			transactions = new ArrayList<>();
			List<String> months = Utils.monthsBack(new Date(), 12, userId + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
			Page<Blob> transactionFiles = storageInfo.getStorage().list(storageInfo.getBucket(), BlobListOption.prefix(userId + "/" + Utils.TRANSACTIONS_PREFIX));
			for (Blob transactionFile : transactionFiles.iterateAll()) {
			    if (months.contains(transactionFile.getName())) {
			        try (ReadChannel readChannel = transactionFile.reader();
	                        BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
	                    List<CsvTransactionRow> csv = CsvUtil.readCsvTransactionRows(true, ",", reader);
	                    transactions.addAll(csv);
	                }
			    }
			}
		} else {
			try (ReadChannel readChannel = retrieve.reader();
                    BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				transactions = CsvUtil.readCsvTransactionRows(true, ",", reader);
            }
		}
		return transactions;
	}

	@Override
	public List<CsvRow> loadPreviousRows(Exchange exchange, int hoursBack, List<String> whiteListSymbols) throws IOException {
		// Get 1 day more and compare dates later
		List<String> days = Utils.daysBack(new Date(), (hoursBack / 24) + 1, "data" + exchange.getFolder(), ".csv");
		List<CsvRow> rows = new ArrayList<>();
		Date now = new Date();
		Date from = Utils.getDateOfHoursBack(now, hoursBack);
		List<CsvRow> csvInDay = null;
		for (String day : days) {
			try (ReadChannel readChannel = storageInfo.getStorage().reader(storageInfo.getBucket(), day);
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				csvInDay = CsvUtil.readCsvRows(true, ",", reader, whiteListSymbols);
				csvInDay = csvInDay.stream().filter(row -> row.getDate().getTime() > from.getTime())
						.collect(Collectors.toList());
				rows.addAll(csvInDay);
			}
		}
		return rows;
	}

	@Override
	public List<CsvProfitRow> loadCsvProfitRows(String userId, int monthsBack) {
		List<String> months = Utils.monthsBack(new Date(), monthsBack, userId + "/" + CsvProfitRow.PREFIX, ".csv");
		List<CsvProfitRow> rows = new ArrayList<>();
		List<CsvProfitRow> csvInMonth = null;
		for (String month : months) {
			try (ReadChannel readChannel = storageInfo.getStorage().reader(storageInfo.getBucket(), month);
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				csvInMonth = CsvUtil.readCsvProfitRows(reader);
				rows.addAll(csvInMonth);
			} catch (Exception e) {
				// Eat it, no CSV was found is not an error
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
		BlobInfo blobInfo = storageInfo.getStorage().create(createBlobInfo(storageInfo, fileName, false), outputStream.toByteArray());
		return blobInfo.getMediaLink();
	}

}
