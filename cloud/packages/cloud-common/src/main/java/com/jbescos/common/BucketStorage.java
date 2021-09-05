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
import com.jbescos.common.BinanceAPI.Interval;

public class BucketStorage {
	
	private static final Logger LOGGER = Logger.getLogger(BucketStorage.class.getName());
	private final Storage storage;
	private final BinanceAPI binanceAPI;
	
	public BucketStorage(Storage storage, BinanceAPI binanceAPI) {
	    this.storage = storage;
	    this.binanceAPI = binanceAPI;
	}
	
	public Map<String, CsvRow> previousRowsUpdatedKline(long serverTime) throws IOException{
		Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
		Map<String, CsvRow> previousRows = new LinkedHashMap<>();
		Blob retrieve = storage.get(CloudProperties.BUCKET, Utils.LAST_PRICE);
		if (retrieve == null) {
			LOGGER.warning(Utils.CSV_ROW_HEADER + " was not found");
			// TODO It is better to get it from the last records of the date.csv
		} else {
			String klineLogMessage = null;
			try (ReadChannel readChannel = retrieve.reader();
					BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, Utils.UTF8));) {
				List<CsvRow> csv = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
				for (CsvRow row : csv) {
					previousRows.put(row.getSymbol(), row);
					Interval interval = Interval.getInterval(row.getDate().getTime(), serverTime);
					long from = interval.from(row.getDate().getTime());
			    	long to = interval.to(from);
	                List<Kline> klines = binanceAPI.klines(interval, row.getSymbol(), null, from, to);
	                if (!klines.isEmpty()) {
	                    Kline kline = klines.get(0);
	                    row.setKline(kline);
	                    if (klineLogMessage == null) {
		                    klineLogMessage = "Klines obtained for data between " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(from)) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(to)) +
		                			" and received from " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(kline.getOpenTime()))
		                			+ " to " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(kline.getCloseTime()));
	                    }
	                } else {
	                	// Ignore it
//	                    LOGGER.warning(row.getSymbol() + ". KLine was not found from " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(from)) + " to " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(to)));
	                }
				}
			}
			if (klineLogMessage != null) {
				LOGGER.info(klineLogMessage);
			}
		}
		return previousRows;
	}

	public List<CsvRow> updatedRowsAndSaveLastPrices(Map<String, CsvRow> previousRows, List<Price> prices, Date now) {
	    Storage storage = StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService();
	    StringBuilder builder = new StringBuilder(Utils.CSV_ROW_HEADER);
	    List<CsvRow> newRows = new ArrayList<>();
        for (Price price : prices) {
            CsvRow previous = previousRows.get(price.getSymbol());
            CsvRow newRow = null;
            if (previous != null) {
                newRow = new CsvRow(now, price.getSymbol(), price.getPrice(), Utils.ewma(CloudProperties.EWMA_CONSTANT, price.getPrice(), previous.getAvg()), Utils.ewma(CloudProperties.EWMA_2_CONSTANT, price.getPrice(), previous.getAvg2()));
                newRow.setKline(previous.getKline());
            } else {
                newRow = new CsvRow(now, price.getSymbol(), price.getPrice(), price.getPrice(), price.getPrice());
            }
            newRows.add(newRow);
            builder.append(newRow.toCsvLine());
        }
        storage.create(createBlobInfo(Utils.LAST_PRICE, false), builder.toString().getBytes(Utils.UTF8));
        return newRows;
	}

	public String updateFile(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
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
