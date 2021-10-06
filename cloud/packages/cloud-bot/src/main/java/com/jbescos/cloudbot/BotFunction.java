package com.jbescos.cloudbot;

import java.util.Date;
import java.util.List;
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
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.PanicBroker;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;

//Entry: com.jbescos.cloudbot.BotFunction
public class BotFunction implements HttpFunction {

	private static final Logger LOGGER = Logger.getLogger(BotFunction.class.getName());
	private static final String USER_ID_PARAM = "userId";
	private static final String SIDE_PARAM = "side";
	private static final String SYMBOL_PARAM = "symbol";
	private static final String QUANTITY_PARAM = "quantity";
	private static final String QUANTITY_USDT_PARAM = "quantityUsd";
	
	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		String userId = Utils.getParam(USER_ID_PARAM, null, request.getQueryParameters());
		if (userId == null || userId.isEmpty()) {
			response.getWriter().write("Parameter userId is mandatory");
			response.setStatusCode(200);
			response.setContentType("text/plain");
		} else {
			CloudProperties cloudProperties = new CloudProperties(userId);
			Client client = ClientBuilder.newClient();
			BinanceAPI binanceAPI = new BinanceAPI(client);
			BucketStorage storage = new BucketStorage(cloudProperties, StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService(), binanceAPI);
			SecureBinanceAPI api = SecureBinanceAPI.create(cloudProperties, client);
			String side = Utils.getParam(SIDE_PARAM, null, request.getQueryParameters());
			if (side != null) {
				Action action = Action.valueOf(side) ;
				LOGGER.info(() -> "Actively invoked " + side);
				if (Action.SELL_PANIC == action) {
					Date now = new Date();
					List<Broker> stats = new BinanceAPI(client).price().stream()
							.filter(price -> cloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(price.getSymbol()))
							.map(price -> new CsvRow(now, price.getSymbol(), price.getPrice()))
							.map(row -> new PanicBroker(row.getSymbol(), row, 0))
							.collect(Collectors.toList());
					BotExecution bot = BotExecution.binance(cloudProperties, api, storage);
					bot.execute(stats);
					response.getWriter().write(stats.toString());
				} else {
					String symbol = Utils.getParam(SYMBOL_PARAM, null, request.getQueryParameters());
					String quantity = Utils.getParam(QUANTITY_PARAM, null, request.getQueryParameters());
					String quoteOrderQty = Utils.getParam(QUANTITY_USDT_PARAM, null, request.getQueryParameters());
					CsvTransactionRow apiResponse = quoteOrderQty != null ? api.orderUSDT(symbol, Action.valueOf(side), quoteOrderQty) : api.orderSymbol(symbol, Action.valueOf(side), quantity);
					storage.updateFile(cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX + Utils.thisMonth(apiResponse.getDate()) + ".csv", apiResponse.toCsvLine().getBytes(Utils.UTF8), Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
					response.getWriter().write(apiResponse.toString());
				}
			} else {
				List<Broker> stats = BotUtils.loadStatistics(cloudProperties, client, true).stream()
						.filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
				BotExecution bot = BotExecution.binance(cloudProperties, api, storage);
				bot.execute(stats);
				response.getWriter().write(stats.toString());
			}
			response.setStatusCode(200);
			response.setContentType("text/plain");
			client.close();
		}
	}

}
