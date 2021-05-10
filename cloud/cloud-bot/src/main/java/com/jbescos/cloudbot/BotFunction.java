package com.jbescos.cloudbot;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

//Entry: com.jbescos.cloudbot.BotFunction
public class BotFunction implements HttpFunction {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		// FIXME Use 'now' instead of 1 day back
	    Calendar c = Calendar.getInstance();
	    c.setTime(new Date());
	    c.add(Calendar.DAY_OF_YEAR, -1);
	    List<SymbolMinMax> minMax = BotUtils.loadPredictions(c.getTime());
	    response.setStatusCode(200);
	    response.setContentType("text/plain");
	    response.getWriter().write(minMax.toString());
	    response.getWriter().flush();
	    
	}

}
