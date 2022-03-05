package com.jbescos.cloudbot;

import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.Broker;
import com.jbescos.common.Broker.Action;
import com.jbescos.common.BrokerManager;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvProfitRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.DefaultBrokerManager;
import com.jbescos.common.PublicAPI;
import com.jbescos.common.SecuredAPI;
import com.jbescos.common.SellPanicBrokerManager;
import com.jbescos.common.StorageInfo;
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
			StorageInfo storageInfo = StorageInfo.build();
			CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
			Client client = ClientBuilder.newClient();
			BucketStorage storage = new BucketStorage(storageInfo);
			BrokerManager brokerManager;
			SecuredAPI api = cloudProperties.USER_EXCHANGE.create(cloudProperties, client);
			String side = Utils.getParam(SIDE_PARAM, null, request.getQueryParameters());
			Action action = Action.valueOf(side) ;
			LOGGER.info(() -> "Actively invoked " + side);
			if (Action.SELL_PANIC == action) {
			    brokerManager = new SellPanicBrokerManager(cloudProperties, storage);
			    BotExecution bot = BotExecution.production(cloudProperties, api, storage);
		        bot.execute(brokerManager.loadBrokers());
			} else {
			    brokerManager = new DefaultBrokerManager(cloudProperties, storage);
				String symbol = Utils.getParam(SYMBOL_PARAM, null, request.getQueryParameters());
				String quantity = Utils.getParam(QUANTITY_PARAM, null, request.getQueryParameters());
				String quoteOrderQty = Utils.getParam(QUANTITY_USDT_PARAM, null, request.getQueryParameters());
				PublicAPI publicApi = new PublicAPI(client);
				Map<String, Double> prices = cloudProperties.USER_EXCHANGE.price(publicApi);
				double currentUsdtPrice = prices.get(symbol);
				CsvTransactionRow apiResponse = quoteOrderQty != null ? api.orderUSDT(symbol, Action.valueOf(side), quoteOrderQty, currentUsdtPrice) : api.orderSymbol(symbol, Action.valueOf(side), quantity, currentUsdtPrice);
				String month = Utils.thisMonth(apiResponse.getDate());
				if (apiResponse.getSide() == Action.SELL) {
					Broker broker = brokerManager.loadBrokers().stream().filter(b -> symbol.equals(b.getSymbol())).findFirst().get();
					CsvProfitRow row = CsvProfitRow.build(cloudProperties.BROKER_COMMISSION, broker.getPreviousTransactions(), apiResponse);
					if (row != null) {
						StringBuilder profitData = new StringBuilder();
						profitData.append(row.toCsvLine());
						storage.updateFile(cloudProperties.USER_ID + "/" + CsvProfitRow.PREFIX + month + ".csv", profitData.toString().getBytes(Utils.UTF8), CsvProfitRow.HEADER.getBytes(Utils.UTF8));
					}
				}
				storage.updateFile(cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX + month + ".csv", apiResponse.toCsvLine().getBytes(Utils.UTF8), Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
				response.getWriter().write(apiResponse.toString());
			}
			response.setStatusCode(200);
			response.setContentType("text/plain");
			client.close();
		}
	}

}
