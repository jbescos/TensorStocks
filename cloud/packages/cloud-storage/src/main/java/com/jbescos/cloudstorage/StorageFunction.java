package com.jbescos.cloudstorage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.api.gax.paging.Page;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.Price;
import com.jbescos.common.PublisherMgr;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

    private static final Logger LOGGER = Logger.getLogger(StorageFunction.class.getName());
	private static final byte[] CSV_HEADER_TOTAL = Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8);

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		CloudProperties cloudProperties = new CloudProperties();
		Client client = ClientBuilder.newClient();
		BinanceAPI binanceAPI = new BinanceAPI(client);
		BucketStorage storage = new BucketStorage(cloudProperties, StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService(), binanceAPI);
		List<String> userIds = new ArrayList<>();
		Page<Blob> files = storage.list(cloudProperties.PROPERTIES_BUCKET);
		for (Blob blob : files.iterateAll()) {
			if (blob.isDirectory()) {
				userIds.add(blob.getName().replaceAll("/", ""));
			}
		}
		long time = binanceAPI.time();
		Date now = new Date(time);
		LOGGER.info(() -> "Server time is: " + Utils.fromDate(Utils.FORMAT_SECOND, now));
		Map<String, CsvRow> previousRows = storage.previousRowsUpdatedKline(time);
		try (PublisherMgr publisher = PublisherMgr.create(cloudProperties)) {
		    // Update current prices
    		List<Price> prices = binanceAPI.price();
            String fileName = Utils.FORMAT.format(now) + ".csv";
    		List<CsvRow> updatedRows = storage.updatedRowsAndSaveLastPrices(previousRows, prices, now);
    		StringBuilder builder = new StringBuilder();
    		for (CsvRow row : updatedRows) {
    			builder.append(row.toCsvLine());
    		}
    		String downloadLink = storage.updateFile("data/" + fileName, builder.toString().getBytes(Utils.UTF8), CSV_HEADER_TOTAL);
    		// Notify bot
    		LOGGER.info("Sending bot messages to " + userIds);
    		publisher.publish(userIds.toArray(new String[0]));
            client.close();
            response.setStatusCode(200);
            response.getWriter().write(downloadLink);
        }
	}

}
