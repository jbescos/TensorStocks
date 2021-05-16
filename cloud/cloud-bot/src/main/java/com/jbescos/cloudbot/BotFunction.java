package com.jbescos.cloudbot;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.Utils;
import com.jbescos.common.SymbolStats.Action;

//Entry: com.jbescos.cloudbot.BotFunction
public class BotFunction implements HttpFunction {

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String daysBack = Utils.getParam("days", "5", request.getQueryParameters());
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DAY_OF_YEAR, Integer.parseInt(daysBack) * -1);
		List<SymbolStats> stats = BotUtils.loadPredictions(c.getTime(), true).stream()
				.filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
		BotBinance bot = new BotBinance(SecureBinanceAPI.create());
		bot.execute(stats);
		response.setStatusCode(200);
		response.setContentType("text/plain");
		response.getWriter().write(stats.toString());
		response.getWriter().flush();

	}

}
