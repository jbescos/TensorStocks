package com.jbescos.cloudbot;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.Broker;
import com.jbescos.common.Broker.Action;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.PanicBroker;
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
		Client client = ClientBuilder.newClient();
		BinanceAPI binanceAPI = new BinanceAPI(client);
		BucketStorage storage = new BucketStorage(StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService(), binanceAPI);
		SecureBinanceAPI api = SecureBinanceAPI.create(client, storage);
		String side = Utils.getParam(SIDE_PARAM, null, request.getQueryParameters());
		if (side != null) {
			Action action = Action.valueOf(side) ;
			LOGGER.info(() -> "Actively invoked " + side);
			if (Action.SELL_PANIC == action) {
				Date now = new Date();
				List<Broker> stats = new BinanceAPI(client).price().stream()
						.filter(price -> CloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(price.getSymbol()))
						.map(price -> new CsvRow(now, price.getSymbol(), price.getPrice()))
						.map(row -> new PanicBroker(row.getSymbol(), row, 0))
						.collect(Collectors.toList());
				BotBinance bot = new BotBinance(api);
				bot.execute(stats);
				response.getWriter().write(stats.toString());
			} else {
				String symbol = Utils.getParam(SYMBOL_PARAM, null, request.getQueryParameters());
				String quantity = Utils.getParam(QUANTITY_PARAM, null, request.getQueryParameters());
				Map<String, String> apiResponse = api.orderSymbol(symbol, Action.valueOf(side), quantity);
				response.getWriter().write(apiResponse.toString());
			}
		} else {
			List<Broker> stats = BotUtils.loadStatistics(client, true).stream()
					.filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
			BotBinance bot = new BotBinance(api);
			bot.execute(stats);
			response.getWriter().write(stats.toString());
		}
		response.setStatusCode(200);
		response.setContentType("text/plain");
		client.close();
	}

}
