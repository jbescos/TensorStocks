package com.jbescos.cloudstorage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.api.gax.paging.Page;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.CsvRow;
import com.jbescos.common.Price;
import com.jbescos.common.PublicAPI;
import com.jbescos.common.PublisherMgr;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

    private static final Logger LOGGER = Logger.getLogger(StorageFunction.class.getName());
	private static final byte[] CSV_HEADER_TOTAL = Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8);
	private static final String SKIP_PARAM = "skip";

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		boolean skip = Boolean.parseBoolean(Utils.getParam(SKIP_PARAM, "false", request.getQueryParameters()));
		CloudProperties cloudProperties = new CloudProperties();
		Client client = ClientBuilder.newClient();
		PublicAPI publicAPI = new PublicAPI(client);
		BucketStorage storage = new BucketStorage(cloudProperties, StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService());
		Map<String, List<String>> groupedFolder = new HashMap<>();
		Page<Blob> files = storage.list(cloudProperties.PROPERTIES_BUCKET);
		for (Blob blob : files.iterateAll()) {
			if (blob.isDirectory()) {
				String userId = blob.getName().replaceAll("/", "");
				try {
					CloudProperties user = new CloudProperties(userId);
					List<String> usersByFolder = groupedFolder.get(user.USER_EXCHANGE.getFolder());
					if (usersByFolder == null) {
						usersByFolder = new ArrayList<>();
						groupedFolder.put(user.USER_EXCHANGE.getFolder(), usersByFolder);
					}
					usersByFolder.add(userId);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Cannot load user " + userId, e);
				}
			}
		}
		long time = publicAPI.time();
		Date now = new Date(time);
		LOGGER.info(() -> "Server time is: " + Utils.fromDate(Utils.FORMAT_SECOND, now));
		try (PublisherMgr publisher = PublisherMgr.create(cloudProperties)) {
			if (!skip) {
				Set<String> updatedExchanges = new HashSet<>();
				for (Exchange exchange : Exchange.values()) {
					if (updatedExchanges.add(exchange.getFolder())) {
						try {
							String lastPrice = "data" + exchange.getFolder() + Utils.LAST_PRICE;
							Map<String, CsvRow> previousRows = storage.previousRows(time, lastPrice);
						    // Update current prices
				    		List<Price> prices = exchange.price(publicAPI);
				            String fileName = Utils.FORMAT.format(now) + ".csv";
				    		List<CsvRow> updatedRows = storage.updatedRowsAndSaveLastPrices(previousRows, prices, now, lastPrice);
				    		StringBuilder builder = new StringBuilder();
				    		for (CsvRow row : updatedRows) {
				    			builder.append(row.toCsvLine());
				    		}
				    		String downloadLink = storage.updateFile("data" + exchange.getFolder() + fileName, builder.toString().getBytes(Utils.UTF8), CSV_HEADER_TOTAL);
				    		// Notify bot
				    		List<String> userIds = groupedFolder.get(exchange.getFolder());
				    		if (userIds != null) {
					    		LOGGER.info("Sending bot messages to " + userIds + " for " + exchange.getFolder());
					    		publisher.publish(userIds.toArray(new String[0]));
				    		}
				    		response.getWriter().write("<" + downloadLink + ">");
						} catch (Exception e) {
							response.getWriter().write("<ERROR: " + exchange.name() + ". " + e.getMessage() + ">");
							LOGGER.log(Level.SEVERE, "Cannot process " + exchange.name(), e);
						}
					}
				}
			} else {
				response.getWriter().write("Skipped");
			}
            client.close();
            response.setStatusCode(200);
        }
	}

}
