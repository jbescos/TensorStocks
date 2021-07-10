package com.jbescos.cloudstorage;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.Account;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.ExchangeInfo;
import com.jbescos.common.Price;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

	private static final byte[] CSV_HEADER_TOTAL = Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8);
	private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		Client client = ClientBuilder.newClient();
		BinanceAPI binanceAPI = new BinanceAPI(client);
		ExchangeInfo exchangeInfo = binanceAPI.exchangeInfo("BTCUSDT");
		Date now = new Date(exchangeInfo.getServerTime());
		String fileName = Utils.FORMAT.format(now) + ".csv";
		List<Price> prices = binanceAPI.price();
		StringBuilder builder = new StringBuilder();
		List<CsvRow> updatedRows = BucketStorage.withAvg(binanceAPI, now, prices);
		for (CsvRow row : updatedRows) {
			builder.append(row.toCsvLine());
		}
		String downloadLink = BucketStorage.updateFile("data/" + fileName, builder.toString().getBytes(Utils.UTF8), CSV_HEADER_TOTAL);
		SecureBinanceAPI api = SecureBinanceAPI.create(client);
		Account account = api.account();
		List<Map<String, String>> rows = Utils.userUsdt(now, prices, account);
		BucketStorage.updateFile("wallet/account_" + fileName, CsvUtil.toString(rows).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
		client.close();
		response.setStatusCode(200);
		response.getWriter().write(downloadLink);
		
	}

}
