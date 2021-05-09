package com.jbescos.cloudstorage;

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
	private static final String TEMP_FILE = "tmp.csv";
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

	public static String updateFile(String fileName, byte[] content, byte[] header)
			throws FileNotFoundException, IOException {
		Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
		BlobInfo retrieve = storage.get(BlobInfo.newBuilder(BUCKET, fileName).build().getBlobId());
		if (retrieve == null) {
			retrieve = storage.create(createBlobInfo(fileName, false), content);
			updateTotalCsv(storage, header, fileName);
			return retrieve.getMediaLink();
		} else {
			storage.create(createBlobInfo(TEMP_FILE, false), content);
			BlobInfo blobInfo = createBlobInfo(fileName, false);
			ComposeRequest request = ComposeRequest.newBuilder().setTarget(blobInfo)
					.addSource(fileName).addSource(TEMP_FILE).build();
			blobInfo = storage.compose(request);
			updateTotalCsv(storage, header, TEMP_FILE);
			return blobInfo.getMediaLink();
		}
	}

	private static void updateTotalCsv(Storage storage, byte[] header, String newDataFile) {
		BlobInfo total = storage.get(BlobInfo.newBuilder(BUCKET, TOTAL_FILE).build().getBlobId());
		if (total == null) {
			total = storage.create(createBlobInfo(TOTAL_FILE, false), header);
		}
		ComposeRequest request = ComposeRequest.newBuilder().setTarget(total).addSource(TOTAL_FILE).addSource(newDataFile).build();
		storage.compose(request);
	}

	private static BlobInfo createBlobInfo(String fileName, boolean acl) {
		BlobInfo.Builder builder = BlobInfo.newBuilder(BUCKET, fileName);
		if (acl) {
			builder.setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.READER))));
		}
		builder.setContentType("text/csv").setContentEncoding(Utils.UTF.name()).setStorageClass(StorageClass.STANDARD);
		return builder.build();
	}

}
