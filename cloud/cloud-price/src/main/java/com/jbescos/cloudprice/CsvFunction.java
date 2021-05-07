package com.jbescos.cloudprice;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.DataBase;
import com.jbescos.common.Utils;

//Entry: com.jbescos.cloudprice.CsvFunction
public class CsvFunction implements HttpFunction {

	private static final String FROM_PARAM = "from";
	private static final String TO_PARAM = "to";

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		Map<String, List<String>> parameters = request.getQueryParameters();
		Date from = getParam(FROM_PARAM, parameters, new Date(0));
		Date to = getParam(TO_PARAM, parameters, new Date());
		String fileName = getFileName(parameters);
		List<Map<String, Object>> csv = new DataBase()
				.get("SELECT * FROM PRICE_HISTORY WHERE DATE BETWEEN ? AND ? ORDER BY DATE ASC", from, to);
		response.setContentType("text/csv");
		response.appendHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		CsvUtil.writeCsv(csv, ',', response.getOutputStream());
		response.getOutputStream().flush();
	}

	private String getFileName(Map<String, List<String>> parameters) {
		String fileName = "PricesHistory.csv";
		List<String> values = parameters.get(TO_PARAM);
		if (values != null && !values.isEmpty()) {
			fileName = values.get(0) + "_" + fileName; 
		}
		values = parameters.get(FROM_PARAM);
		if (values != null && !values.isEmpty()) {
			fileName = values.get(0) + "_" + fileName; 
		}
		return fileName;
	}

	private Date getParam(String param, Map<String, List<String>> parameters, Date defaultDate) throws ParseException {
		List<String> values = parameters.get(param);
		if (values == null || values.isEmpty()) {
			return defaultDate;
		} else {
			return Utils.FORMAT.parse(values.get(0));
		}
	}
}
