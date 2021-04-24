package es.tododev.stocks.yahoo;

import java.util.Collections;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

public class YahooAPI {

	private final String key;

	public YahooAPI(String key) {
		this.key = key;
	}

	public Prices getHistory(String symbol) {
		Client client = ClientBuilder.newClient(new ClientConfig());
		WebTarget webTarget = client
				.target("https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v3/get-historical-data")
				.queryParam("symbol", symbol).queryParam("region", "US");
		Response response = webTarget.request(MediaType.APPLICATION_JSON).header("x-rapidapi-key", key)
				.header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com").get();
		if (response.getStatus() == 200) {
			Prices prices = response.readEntity(Prices.class);
			Collections.reverse(prices.getPrices());
			return prices;
		} else {
			response.bufferEntity();
			throw new RuntimeException("HTTP response code " + response.getStatus() + " from YahooAPI for symbol " + symbol + " : " + response.readEntity(String.class));
		}
	}
}
