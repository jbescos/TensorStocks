package com.jbescos.cloudbot;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BuySellAnalisys;
import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.SecureBinanceAPI;

//Entry: com.jbescos.cloudbot.BotFunction
public class BotFunction implements HttpFunction {

	private static final Logger LOGGER = Logger.getLogger(BotFunction.class.getName());
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		List<BuySellAnalisys> stats = BotUtils.loadStatistics().stream()
				.filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
		BotBinance bot = new BotBinance(SecureBinanceAPI.create());
		bot.execute(stats);
		response.setStatusCode(200);
		response.setContentType("text/plain");
		response.getWriter().write(stats.toString());
		response.getWriter().flush();

	}

}
