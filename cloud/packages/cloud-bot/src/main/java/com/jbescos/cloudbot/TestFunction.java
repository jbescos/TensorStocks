package com.jbescos.cloudbot;

import java.util.Date;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.StorageInfo;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.Utils;

//Entry: com.jbescos.cloudbot.TestFunction
public class TestFunction implements HttpFunction {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		StorageInfo storageInfo = StorageInfo.build();
		BucketStorage storage = new BucketStorage(storageInfo);
		String header = Utils.CSV_ROW_HEADER;
		CsvRow row1 = new CsvRow(new Date(), "test", 0);
		CsvRow row2 = new CsvRow(new Date(), "test2", 0);
		String body1 = row1.toCsvLine() + row2.toCsvLine();
		storage.updateFile("test/test.csv", body1.getBytes(), header.getBytes());
	}

}
