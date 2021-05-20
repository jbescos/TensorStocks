package com.jbescos.cloudcsv;

import java.util.Arrays;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BucketStorage;

// Entry: com.jbescos.cloudcsv.CsvFunction
public class CsvFunction implements HttpFunction {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String fileName = request.getQueryParameters().get("file").get(0);
		String text = BucketStorage.fileToString(fileName);
		response.setStatusCode(200);
		response.setContentType("text/plain");
		response.getHeaders().put("Access-Control-Allow-Origin", Arrays.asList("*"));
		response.getHeaders().put("Access-Control-Request-Headers", Arrays.asList("*"));
		response.getWriter().write(text);
	}

}
