package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Acl.User;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.ComposeRequest;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.Utils;

public class BucketStorage {

	private static final String BUCKET;
	private static final String PROJECT_ID;
	private static final String TOTAL_FILE = "total.csv";
	private static final String ACCOUNT_TOTAL_FILE = "account_total.csv";
	private static final String TRANSACTIONS_TOTAL_FILE = "transactions_total.csv";

	static {
		try {
			Properties properties = Utils.fromClasspath("/storage.properties");
			BUCKET = properties.getProperty("storage.bucket");
			PROJECT_ID = properties.getProperty("project.id");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static String updateFilePrices(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		return updateFile(fileName, TOTAL_FILE, content, header);
	}
	
	public static String updateFileAccount(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		return updateFile(fileName, ACCOUNT_TOTAL_FILE, content, header);
	}
	
	public static String updateFileTransactions(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		return updateFile(fileName, TRANSACTIONS_TOTAL_FILE, content, header);
	}

	public static String updateFile(String fileName, String totalCsv, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
		BlobInfo retrieve = storage.get(BlobInfo.newBuilder(BUCKET, fileName).build().getBlobId());
		if (retrieve == null) {
			retrieve = storage.create(createBlobInfo(fileName, false), content);
			updateTotalCsv(storage, header, fileName, totalCsv);
			return retrieve.getMediaLink();
		} else {
			final String TEMP_FILE = "tmp_" + totalCsv;
			storage.create(createBlobInfo(TEMP_FILE, false), content);
			BlobInfo blobInfo = createBlobInfo(fileName, false);
			ComposeRequest request = ComposeRequest.newBuilder().setTarget(blobInfo)
					.addSource(fileName).addSource(TEMP_FILE).build();
			blobInfo = storage.compose(request);
			updateTotalCsv(storage, header, TEMP_FILE, totalCsv);
			return blobInfo.getMediaLink();
		}
	}

	private static void updateTotalCsv(Storage storage, byte[] header, String newDataFile, String totalCsv) {
		BlobInfo total = storage.get(BlobInfo.newBuilder(BUCKET, totalCsv).build().getBlobId());
		if (total == null) {
			total = storage.create(createBlobInfo(totalCsv, false), header);
		}
		ComposeRequest request = ComposeRequest.newBuilder().setTarget(total).addSource(totalCsv).addSource(newDataFile).build();
		storage.compose(request);
	}

	private static BlobInfo createBlobInfo(String fileName, boolean acl) {
		BlobInfo.Builder builder = BlobInfo.newBuilder(BUCKET, fileName);
		if (acl) {
			builder.setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.READER))));
		}
		builder.setContentType("text/csv").setContentEncoding(Utils.UTF8.name()).setStorageClass(StorageClass.STANDARD);
		return builder.build();
	}

}
