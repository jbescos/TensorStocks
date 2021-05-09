package com.jbescos.cloudchart;

import java.util.Collections;
import java.util.List;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {

	private static final String SYMBOL_PARAM = "symbol";
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		List<String> symbols = request.getQueryParameters().get(SYMBOL_PARAM);
		if (symbols == null) {
			symbols = Collections.emptyList();
		}
		response.setContentType("image/png");
		response.appendHeader("Content-Disposition", "attachment; filename=\"chart.png\"");
		ChartGenerator.writeChart(response.getOutputStream(), symbols);
		response.getOutputStream().flush();
	}

}
