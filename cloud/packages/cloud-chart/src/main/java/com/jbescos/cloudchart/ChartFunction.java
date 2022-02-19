package com.jbescos.cloudchart;

import java.util.List;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.cloudchart.ChartGenerator.AccountChartCsv;
import com.jbescos.cloudchart.ChartGenerator.IChartCsv;
import com.jbescos.cloudchart.ChartGenerator.ProfitableBarChartCsv;
import com.jbescos.cloudchart.ChartGenerator.SymbolChartCsv;
import com.jbescos.cloudchart.ChartGenerator.TxSummaryChartCsv;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.Utils;

// Entry: com.jbescos.cloudchart.ChartFunction
public class ChartFunction implements HttpFunction {
	
	private static final String HTML_PAGE = "<html>\n" + 
			"<body>\n" + 
			"\n" + 
			"<h1><USER_ID> Report</h1>\n" + 
			" <img src=\"https://alternative.me/crypto/fear-and-greed-index.png\" alt=\"image\"/> \n" + 
			"<h2>Summary of benefits</h2>\n" + 
			" <img src=\"<CHART_URL>?userId=<USER_ID>&type=summary&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/> \n" + 
			"<h2>Wallet 7 days</h2>\n" + 
			" <img src=\"<CHART_URL>?userId=<USER_ID>&days=7&uncache=<TIMESTAMP>\" alt=\"image\"/> \n" + 
			"<h2>Wallet 365 days</h2>\n" + 
			" <img src=\"<CHART_URL>?userId=<USER_ID>&days=365&uncache=<TIMESTAMP>\" alt=\"image\"/> \n" + 
			"</body>\n" + 
			"</html>";
	private static final String USER_ID_PARAM = "userId";
	private static final String TYPE_LINE = "line";
	private static final String TYPE_BAR = "bar";
	private static final String TYPE_SUMMARY = "summary";
	private static final String TYPE_HTML = "html";
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String userId = Utils.getParam(USER_ID_PARAM, null, request.getQueryParameters());
		if (userId == null || userId.isEmpty()) {
			response.getWriter().write("Parameter userId is mandatory");
			response.setStatusCode(200);
			response.setContentType("text/plain");
		} else {
			CloudProperties cloudProperties = new CloudProperties(userId);
			String type = Utils.getParam("type", TYPE_LINE, request.getQueryParameters());
			if (TYPE_HTML.equals(type)) {
				response.setContentType("text/html");
				String htmlPage = HTML_PAGE.replaceAll("<USER_ID>", userId).replaceAll("<USER_ID>", userId).replaceAll("<TIMESTAMP>", Long.toString(System.currentTimeMillis()));
				response.getWriter().append(htmlPage);
				response.getWriter().flush();
			} else {
				String daysBack = Utils.getParam("days", "365", request.getQueryParameters());
				List<String> symbols = request.getQueryParameters().get("symbol");
				IChartCsv chart = null;
				if (TYPE_LINE.equals(type)) {
					if (symbols == null || symbols.isEmpty()) {
						chart = new AccountChartCsv(cloudProperties);
					} else {
						chart = new SymbolChartCsv(cloudProperties, symbols);
					}
				} else if (TYPE_BAR.equals(type)) {
					chart = new ProfitableBarChartCsv(cloudProperties, symbols);
				} else if (TYPE_SUMMARY.equals(type)) {
					chart = new TxSummaryChartCsv(cloudProperties);
				} else {
					throw new IllegalArgumentException("Unknown type=" + type);
				}
				String fileName = chart.getClass().getSimpleName() + "_" + Utils.today() + ".png";
				response.setContentType("image/png");
				response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
				ChartGenerator.writeLoadAndWriteChart(response.getOutputStream(), Integer.parseInt(daysBack), chart);
				response.getOutputStream().flush();
			}
		}
	}

}
