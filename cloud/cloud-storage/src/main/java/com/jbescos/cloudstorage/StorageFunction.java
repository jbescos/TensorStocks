package com.jbescos.cloudstorage;

import java.util.Date;
import java.util.List;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.ExchangeInfo;
import com.jbescos.common.Price;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

	private static final byte[] CSV_HEADER = "DATE,SYMBOL,PRICE\r\n".getBytes(Utils.UTF8);

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		ExchangeInfo exchangeInfo = BinanceAPI.exchangeInfo();
		Date now = new Date(exchangeInfo.getServerTime());
		String dateStr = Utils.FORMAT_SECOND.format(now);
		String fileName = Utils.FORMAT.format(now) + ".csv";
		List<Price> prices = BinanceAPI.price();
		StringBuilder builder = new StringBuilder();
		for (Price price : prices) {
			builder.append(dateStr).append(",").append(price.getSymbol()).append(",").append(price.getPrice()).append("\r\n");
		}
		prices = null;
		String downloadLink = BucketStorage.updateFile(fileName, builder.toString().getBytes(Utils.UTF8), CSV_HEADER);
		response.setStatusCode(200);
		response.getWriter().write(downloadLink);
		
	}

}
