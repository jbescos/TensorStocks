package com.jbescos.cloudchart;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		response.setContentType("image/png");
		response.appendHeader("Content-Disposition", "attachment; filename=\"chart.png\"");
		ChartGenerator.writeChart(response.getOutputStream());
		response.getOutputStream().flush();
	}

}
