package com.jbescos.cloudstorage;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.Price;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudstorage.StorageFunction
public class StorageFunction implements HttpFunction {

	private static final byte[] CSV_HEADER = "DATE,SYMBOL,PRICE\r\n".getBytes(Utils.UTF8);

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		Date date = new Date();
		String dateStr = Utils.FORMAT_SECOND.format(date);
		String fileName = Utils.FORMAT.format(date) + ".csv";
		StringBuilder builder = new StringBuilder();
		List<Price> prices = BinanceAPI.get("/api/v3/ticker/price", null, new GenericType<List<Price>>() {});
		prices = prices.stream().filter(price -> price.getSymbol().endsWith("USDT")).collect(Collectors.toList());
		for (Price price : prices) {
			builder.append(dateStr).append(",").append(price.getSymbol()).append(",").append(price.getPrice()).append("\r\n");
		}
		prices = null;
		String downloadLink = BucketStorage.updateFile(fileName, builder.toString().getBytes(Utils.UTF8), CSV_HEADER);
		response.setStatusCode(200);
		response.getWriter().write(downloadLink);
		
	}

}
