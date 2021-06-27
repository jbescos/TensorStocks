package com.jbescos.cloudbot;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BuySellAnalisys;
import com.jbescos.common.BuySellAnalisys.Action;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

//Entry: com.jbescos.cloudbot.BotFunction
public class BotFunction implements HttpFunction {

	private static final Logger LOGGER = Logger.getLogger(BotFunction.class.getName());
	private static final String SIDE_PARAM = "side";
	private static final String SYMBOL_PARAM = "symbol";
	private static final String QUANTITY_PARAM = "quantity";
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		SecureBinanceAPI api = SecureBinanceAPI.create();
		String side = Utils.getParam(SIDE_PARAM, null, request.getQueryParameters());
		if (side != null) {
			LOGGER.info("Actively invoked to sell or buy");
			String symbol = Utils.getParam(SYMBOL_PARAM, null, request.getQueryParameters());
			String quantity = Utils.getParam(QUANTITY_PARAM, null, request.getQueryParameters());
			Map<String, String> apiResponse = api.orderSymbol(symbol, side, quantity);
			response.getWriter().write(apiResponse.toString());
		} else {
			List<BuySellAnalisys> stats = BotUtils.loadStatistics().stream()
					.filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
			BotBinance bot = new BotBinance(api);
			bot.execute(stats);
			response.getWriter().write(stats.toString());
		}
		response.setStatusCode(200);
		response.setContentType("text/plain");
	}

}
