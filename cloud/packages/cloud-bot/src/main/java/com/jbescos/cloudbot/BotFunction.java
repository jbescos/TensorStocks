package com.jbescos.cloudbot;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.SymbolStats;
import com.jbescos.common.SymbolStats.Action;
import com.jbescos.common.Utils;

//Entry: com.jbescos.cloudbot.BotFunction
public class BotFunction implements HttpFunction {

	private static final Logger LOGGER = Logger.getLogger(BotFunction.class.getName());
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String daysBack = Utils.getParam("days", CloudProperties.BOT_DAYS_BACK_STATISTICS, request.getQueryParameters());
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
