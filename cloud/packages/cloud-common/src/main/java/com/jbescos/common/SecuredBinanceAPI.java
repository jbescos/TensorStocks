package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.common.Account.Balances;
import com.jbescos.common.Broker.Action;

public class SecuredBinanceAPI implements SecuredAPI {

	private static final Logger LOGGER = Logger.getLogger(SecuredBinanceAPI.class.getName());
	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	private static final String HMAC_SHA_256 = "HmacSHA256";
	private static final String URL = "https://api.binance.com";
	private final Mac mac;
	private final String publicKey;
	private final Client client;

	private SecuredBinanceAPI(Client client, String publicKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
		mac = Mac.getInstance(HMAC_SHA_256);
		mac.init(new SecretKeySpec(privateKey.getBytes(), HMAC_SHA_256));
		this.publicKey = publicKey;
		this.client = client;
	}

	public String signature(String data) {
		return bytesToHex(mac.doFinal(data.getBytes()));
	}

	private String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0, v; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	public <T> T get(String path, GenericType<T> type, String... query) {
		WebTarget webTarget = client.target(URL).path(path);
		StringBuilder queryStr = new StringBuilder();
		if (query.length != 0) {
			for (int i = 0; i < query.length; i = i + 2) {
				String key = query[i];
				String value = query[i + 1];
				webTarget = webTarget.queryParam(key, value);
				if (i != 0) {
					queryStr.append("&");
				}
				queryStr.append(key).append("=").append(value);
			}
			String signature = signature(queryStr.toString());
			webTarget = webTarget.queryParam("signature", signature);
		}
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header("X-MBX-APIKEY", publicKey);
		try (Response response = builder.get()) {
			if (response.getStatus() == 200) {
				return response.readEntity(type);
			} else {
				response.bufferEntity();
				throw new RuntimeException("SecuredBinanceAPI> HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
						+ webTarget.toString() + " : " + response.readEntity(String.class));
			}
		}
	}
	
	public <T> T post(String path, GenericType<T> type, String... query) {
		WebTarget webTarget = client.target(URL).path(path);
		StringBuilder queryStr = new StringBuilder();
		if (query.length != 0) {
			for (int i = 0; i < query.length; i = i + 2) {
				String key = query[i];
				String value = query[i + 1];;
				if (i != 0) {
					queryStr.append("&");
				}
				queryStr.append(key).append("=").append(value);
			}
			String signature = signature(queryStr.toString());
			queryStr.append("&signature=").append(signature);
		}
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header("X-MBX-APIKEY", publicKey);
		try (Response response = builder.post(Entity.text(queryStr.toString()))) {
			if (response.getStatus() == 200) {
				return response.readEntity(type);
			} else {
				response.bufferEntity();
				throw new RuntimeException("SecuredBinanceAPI> HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
						+ webTarget.toString() + " : " + response.readEntity(String.class));
			}
		}
	}

	public Account account() {
		Account account = get("/api/v3/account", new GenericType<Account>() {}, "timestamp", Long.toString(new Date().getTime()));
		List<Balances> balances = account.getBalances().stream().filter(balance -> {
			double free = Double.parseDouble(balance.getFree());
			return free != 0;
		}).collect(Collectors.toList());
		account.setBalances(balances);
		return account;
	}
	
	@Override
	public Map<String, String> wallet(){
		Map<String, String> wallet = new HashMap<>();
		Account account = account();
		for (Balances balance : account.getBalances()) {
			wallet.put(balance.getAsset(), balance.getFree());
		}
		return wallet;
	}
	
	// quoteOrderQty in USDT
	@Override
	public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice) {
		Date now =  new Date();
		String orderId = UUID.randomUUID().toString();
		String[] args = new String[] {"symbol", symbol, "side", action.side(), "type", "MARKET", "quoteOrderQty", quoteOrderQty, "newClientOrderId", orderId, "newOrderRespType", "RESULT", "timestamp", Long.toString(now.getTime())};
		LOGGER.info(() -> "SecuredBinanceAPI> Prepared USDT order: " + Arrays.asList(args).toString());
		Map<String, String> response = post("/api/v3/order", new GenericType<Map<String, String>>() {}, args);
		LOGGER.info(() -> "SecuredBinanceAPI> Completed USDT order: " + response);
		return createTxFromResponse(now, response, action, currentUsdtPrice);
	}
	
	// quantity in units of symbol
	@Override
	public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice) {
		Date now =  new Date();
		String orderId = UUID.randomUUID().toString();
		ExchangeInfo exchange = new PublicAPI(client).exchangeInfo(symbol);
		Map<String, Object> filter = exchange.getFilter(symbol, ExchangeInfo.LOT_SIZE);
		String fixedQuantity = Utils.filterLotSizeQuantity(quantity, filter.get("minQty").toString(), filter.get("maxQty").toString(), filter.get("stepSize").toString());
		String[] args = new String[] {"symbol", symbol, "side", action.side(), "type", "MARKET", "quantity", fixedQuantity, "newClientOrderId", orderId, "newOrderRespType", "RESULT", "timestamp", Long.toString(now.getTime())};
		LOGGER.info(() -> "SecuredBinanceAPI> Prepared Symbol order: " + Arrays.asList(args).toString());
		Map<String, String> response = post("/api/v3/order", new GenericType<Map<String, String>>() {}, args);
		LOGGER.info(() -> "SecuredBinanceAPI> Completed Symbol order: " + response);
		return createTxFromResponse(now, response, action, currentUsdtPrice);
	}

	private CsvTransactionRow createTxFromResponse(Date now, Map<String, String> response, Action action, Double currentUsdtPrice) {
	    // Units of symbol
        String executedQty = response.get("executedQty");
        // USDT
        String cummulativeQuoteQty = response.get("cummulativeQuoteQty");
        if (currentUsdtPrice == null) {
        	// quoteOrderQty / executedQty
        	double quoteOrderQtyBD = Double.parseDouble(cummulativeQuoteQty);
            double executedQtyBD = Double.parseDouble(executedQty);
            currentUsdtPrice = quoteOrderQtyBD/executedQtyBD;
        }
        CsvTransactionRow transaction = new CsvTransactionRow(now, response.get("orderId"), action, response.get("symbol"), cummulativeQuoteQty, executedQty, currentUsdtPrice);
        return transaction;
	}

	public Map<String, String> testOrder(String symbol, String side, String quoteOrderQty) throws FileNotFoundException, IOException {
		Date now =  new Date();
		String orderId = UUID.randomUUID().toString();
		String[] args = new String[] {"symbol", symbol, "side", side, "type", "MARKET", "quoteOrderQty", quoteOrderQty, "newClientOrderId", orderId, "newOrderRespType", "RESULT", "timestamp", Long.toString(now.getTime())};
		LOGGER.info(() -> "SecuredBinanceAPI> TEST. Prepared order: " + Arrays.asList(args).toString());
		Map<String, String> response = post("/api/v3/order/test", new GenericType<Map<String, String>>() {}, args);
		LOGGER.info(() -> "SecuredBinanceAPI> TEST. Completed order: " + response);
		return response;
	}

	public static SecuredBinanceAPI create(Client client, String publicKey, String privateKey)
			throws InvalidKeyException, NoSuchAlgorithmException {
		return new SecuredBinanceAPI(client, publicKey, privateKey);
	}
	
	public static SecuredBinanceAPI create(CloudProperties cloudProperties, Client client)
			throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		return new SecuredBinanceAPI(client, cloudProperties.BINANCE_PUBLIC_KEY, cloudProperties.BINANCE_PRIVATE_KEY);
	}
}
