package com.jbescos.localbot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.localbot.Account.Balances;

public class BinanceRestAPI {
	private static final Logger LOGGER = Logger.getLogger(BinanceRestAPI.class.getName());
	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	private static final String HMAC_SHA_256 = "HmacSHA256";
	private static final String URL = "https://api.binance.com";
	private final Mac mac;
	private final String publicKey;

	private BinanceRestAPI(String publicKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
		mac = Mac.getInstance(HMAC_SHA_256);
		mac.init(new SecretKeySpec(privateKey.getBytes(), HMAC_SHA_256));
		this.publicKey = publicKey;
	}

	private String signature(String data) {
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

	private <T> T get(String path, GenericType<T> type, String... query) {
		Client client = ClientBuilder.newClient();
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
				throw new RuntimeException(
						"HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
								+ webTarget.getUri().toString() + " : " + response.readEntity(String.class));
			}
		}
	}

	private <T> T post(String path, GenericType<T> type, String... query) {
		Client client = ClientBuilder.newClient();
		WebTarget webTarget = client.target(URL).path(path);
		StringBuilder queryStr = new StringBuilder();
		if (query.length != 0) {
			for (int i = 0; i < query.length; i = i + 2) {
				String key = query[i];
				String value = query[i + 1];
				;
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
				throw new RuntimeException(
						"HTTP response code " + response.getStatus() + " with query " + queryStr.toString() + " from "
								+ webTarget.getUri().toString() + " : " + response.readEntity(String.class));
			}
		}
	}

	private Account account() {
		Account account = get("/api/v3/account", new GenericType<Account>() {
		}, "timestamp", Long.toString(new Date().getTime()));
		List<Balances> balances = account.getBalances().stream().filter(balance -> {
			double free = Double.parseDouble(balance.getFree());
			return free != 0;
		}).collect(Collectors.toList());
		account.setBalances(balances);
		return account;
	}

	public ConcurrentHashMap<String, BigDecimal> wallet() {
		ConcurrentHashMap<String, BigDecimal> wallet = new ConcurrentHashMap<>();
		Account account = account();
		for (Balances balance : account.getBalances()) {
			wallet.put(balance.getAsset(), new BigDecimal(balance.getFree()));
		}
		return wallet;
	}

	// quoteOrderQty is always in USDT !!!
	public Transaction order(String symbol, String side, String quoteOrderQty)
			throws FileNotFoundException, IOException {
		Date now = new Date();
		String orderId = UUID.randomUUID().toString();
		String[] args = new String[] { "symbol", symbol, "side", side, "type", "MARKET", "quoteOrderQty", quoteOrderQty,
				"newClientOrderId", orderId, "newOrderRespType", "RESULT", "timestamp", Long.toString(now.getTime()) };
		LOGGER.info("Prepared order: " + Arrays.asList(args).toString());
		Map<String, String> response = post("/api/v3/order", new GenericType<Map<String, String>>() {
		}, args);
		LOGGER.info("Completed order: " + response);
		String executedQty = response.get("executedQty");
		BigDecimal quoteOrderQtyBD = new BigDecimal(quoteOrderQty);
		BigDecimal executedQtyBD = new BigDecimal(executedQty);
		BigDecimal result = quoteOrderQtyBD.divide(executedQtyBD);
		return new Transaction(Constants.FORMAT_SECOND.format(now), orderId, side, symbol, quoteOrderQty, executedQty, result.toString());
	}

	public Map<String, String> testOrder(String symbol, String side, String quoteOrderQty)
			throws FileNotFoundException, IOException {
		Date now = new Date();
		String orderId = UUID.randomUUID().toString();
		String[] args = new String[] { "symbol", symbol, "side", side, "type", "MARKET", "quoteOrderQty", quoteOrderQty,
				"newClientOrderId", orderId, "newOrderRespType", "RESULT", "timestamp", Long.toString(now.getTime()) };
		LOGGER.info("TEST. Prepared order: " + Arrays.asList(args).toString());
		Map<String, String> response = post("/api/v3/order/test", new GenericType<Map<String, String>>() {
		}, args);
		LOGGER.info("TEST. Completed order: " + response);
		return response;
	}

	public static BinanceRestAPI create(String publicKey, String privateKey)
			throws InvalidKeyException, NoSuchAlgorithmException {
		return new BinanceRestAPI(publicKey, privateKey);
	}

	public static BinanceRestAPI create() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		return new BinanceRestAPI(Constants.BINANCE_PUBLIC_KEY, Constants.BINANCE_PRIVATE_KEY);
	}
}
