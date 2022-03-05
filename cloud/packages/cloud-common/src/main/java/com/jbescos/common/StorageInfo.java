package com.jbescos.common;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.BucketListOption;

public class StorageInfo {

    private static final String PREFIX_PROPERTIES_BUCKET = "crypto-properties";
    private static final String PREFIX_STORAGE_BUCKET = "crypto-for-training";
	private final Storage storage;
	private final String projectId;
	private final String bucket;
	private final String propertiesBucket;

	private StorageInfo(Storage storage, String projectId,String bucket, String propertiesBucket) {
		this.storage = storage;
		this.projectId = projectId;
		this.bucket = bucket;
		this.propertiesBucket = propertiesBucket;
	}
	
	public Storage getStorage() {
		return storage;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getBucket() {
		return bucket;
	}

	public String getPropertiesBucket() {
		return propertiesBucket;
	}

	public static StorageInfo build() {
		String projectId = StorageOptions.getDefaultProjectId();
		Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
		String bucket = findByPrefix(storage, PREFIX_STORAGE_BUCKET);
		String propertiesBucket = findByPrefix(storage, PREFIX_PROPERTIES_BUCKET);
		return new StorageInfo(storage, projectId, bucket, propertiesBucket);
	}
	
    private static String findByPrefix(Storage storage, String prefix) {
    	Page<Bucket> page = storage.list(BucketListOption.prefix(prefix));
        for (Bucket bucket : page.iterateAll()) {
            return bucket.getName();
        }
        throw new IllegalStateException("Bucket that starts with " + prefix + " was not found");
    }
}
