package com.jbescos.cloudchart;

import java.util.List;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.cloudchart.ChartGenerator.AccountChartCsv;
import com.jbescos.cloudchart.ChartGenerator.IChartCsv;
import com.jbescos.cloudchart.ChartGenerator.ProfitableBarChartCsv;
import com.jbescos.cloudchart.ChartGenerator.SymbolChartCsv;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {
	
	private static final String TYPE_LINE = "line";
	private static final String TYPE_BAR = "bar";
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String type = Utils.getParam("type", TYPE_LINE, request.getQueryParameters());
		String daysBack = Utils.getParam("days", "365", request.getQueryParameters());
		List<String> symbols = request.getQueryParameters().get("symbol");
		IChartCsv chart = null;
		if (TYPE_LINE.equals(type)) {
			if (symbols == null || symbols.isEmpty()) {
				chart = new AccountChartCsv();
			} else {
				chart = new SymbolChartCsv(symbols);
			}
		} else if (TYPE_BAR.equals(type)) {
			chart = new ProfitableBarChartCsv(symbols);
		} else {
			throw new IllegalArgumentException("Unkown type=" + type);
		}
		String fileName = chart.getClass().getSimpleName() + "_" + Utils.today() + ".png";
		response.setContentType("image/png");
		response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		ChartGenerator.writeLoadAndWriteChart(response.getOutputStream(), Integer.parseInt(daysBack), chart);
		response.getOutputStream().flush();
	}

}
