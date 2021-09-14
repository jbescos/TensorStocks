package com.jbescos.cloudstorage;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.Account;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Price;
import com.jbescos.common.PublisherMgr;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

    private static final Logger LOGGER = Logger.getLogger(StorageFunction.class.getName());
	private static final byte[] CSV_HEADER_TOTAL = Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8);
	private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		Client client = ClientBuilder.newClient();
		BinanceAPI binanceAPI = new BinanceAPI(client);
		BucketStorage storage = new BucketStorage(StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService(), binanceAPI);
		long time = binanceAPI.time();
		Date now = new Date(time);
		LOGGER.info(() -> "Server time is: " + Utils.fromDate(Utils.FORMAT_SECOND, now));
		Map<String, CsvRow> previousRows = storage.previousRowsUpdatedKline(time);
		String message = Utils.fromDate(Utils.FORMAT_SECOND, now);
		try (PublisherMgr publisher = PublisherMgr.create()) {
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
    		publisher.publish(message);
    		// Update wallet
            SecureBinanceAPI api = SecureBinanceAPI.create(client, storage);
            Account account = api.account();
            List<Map<String, String>> rows = Utils.userUsdt(now, prices, account);
            storage.updateFile(Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv", CsvUtil.toString(rows).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
            client.close();
            response.setStatusCode(200);
            response.getWriter().write(downloadLink);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot send message: " + message, e);
        }
		
	}

}
