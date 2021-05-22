package com.jbescos.cloudchart;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String daysBack = Utils.getParam("days", "365", request.getQueryParameters());
		String fileName = "account_" + Utils.today() + ".png";
		response.setContentType("image/png");
		response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		ChartGenerator.writeLoadAndWriteChart(response.getOutputStream(), Integer.parseInt(daysBack));
		response.getOutputStream().flush();
	}

}
