package com.jbescos.cloudchart;

import java.util.List;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.cloudchart.ChartGenerator.AccountChartCsv;
import com.jbescos.cloudchart.ChartGenerator.IChartCsv;
import com.jbescos.cloudchart.ChartGenerator.SymbolChartCsv;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String daysBack = Utils.getParam("days", "365", request.getQueryParameters());
		List<String> symbols = request.getQueryParameters().get("symbol");
		IChartCsv chart = null;
		if (symbols == null || symbols.isEmpty()) {
			chart = new AccountChartCsv();
		} else {
			chart = new SymbolChartCsv(symbols);
		}
		String fileName = chart.getClass().getSimpleName() + "_" + Utils.today() + ".png";
		response.setContentType("image/png");
		response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		ChartGenerator.writeLoadAndWriteChart(response.getOutputStream(), Integer.parseInt(daysBack), chart);
		response.getOutputStream().flush();
	}

}
