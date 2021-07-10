package com.jbescos.cloudprice;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.DataBase;
import com.jbescos.common.Price;

// Entry: com.jbescos.cloudprice.PriceFunction
public class PriceFunction implements HttpFunction {
	private static final Logger LOGGER = Logger.getLogger(PriceFunction.class.getName());

	@Override
	public void service(HttpRequest request, HttpResponse response) throws Exception {
		Client client = ClientBuilder.newClient();
		Date date = new Date();
		List<Price> prices = new BinanceAPI(client).get("/api/v3/ticker/price", new GenericType<List<Price>>() {});
		prices = prices.stream().filter(price -> price.getSymbol().endsWith("USDT")).collect(Collectors.toList());
		try {
			int result = new DataBase().insert("INSERT INTO PRICE_HISTORY (SYMBOL, PRICE, DATE) VALUES (?, ?, ?)", prices, date);
			response.setStatusCode(200);
			response.getWriter().write("Inserted " + result);
		} catch (SQLException e) {
			response.setStatusCode(500);
			response.getWriter().write("Inserted " + e.getMessage());
			LOGGER.log(Level.SEVERE, "Cannot insert " + prices.size() + " records", e);
		}
		response.getWriter().flush();
		client.close();
	}
}