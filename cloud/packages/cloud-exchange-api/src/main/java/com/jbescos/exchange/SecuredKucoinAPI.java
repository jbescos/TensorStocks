package com.jbescos.exchange;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Broker.Action;

public class SecuredKucoinAPI implements SecuredAPI {

	private static final Logger LOGGER = Logger.getLogger(SecuredKucoinAPI.class.getName());
	private static final String KUCOIN_PRODUCTION = "https://api.kucoin.com";
	private static final String API_URL = KUCOIN_PRODUCTION;
	private static final String HMAC_SHA_256 = "HmacSHA256";
	private static final String HEADER_API_KEY = "KC-API-KEY";
	private static final String HEADER_API_SIGN = "KC-API-SIGN";
	private static final String HEADER_API_TIMESTAMP = "KC-API-TIMESTAMP";
	private static final String HEADER_API_PASSPHRASE = "KC-API-PASSPHRASE";
	private static final String HEADER_API_VERSION = "KC-API-KEY-VERSION";
	private final Client client;
	private final String publicKey;
	private final String passphrase;
	private final String version;
	private final Mac mac;
	private final double buyCommission;
	private final double sellCommission;
	
	private SecuredKucoinAPI(Client client, String publicKey, String privateKey, String passphrase, String version, double buyCommission, double sellCommission) throws InvalidKeyException, NoSuchAlgorithmException {
		this.mac = Mac.getInstance(HMAC_SHA_256);
		mac.init(new SecretKeySpec(privateKey.getBytes(), HMAC_SHA_256));
		this.client = client;
		this.publicKey = publicKey;
		this.passphrase = passphrase;
		this.version = version;
		this.buyCommission = buyCommission;
		this.sellCommission = sellCommission;
	}
	
	public <T> T get(String path, GenericType<T> type, String... query) {
		WebTarget webTarget = client.target(API_URL).path(path);
        if (query.length != 0) {
            for (int i = 0; i < query.length; i = i + 2) {
                String key = query[i];
                String value = query[i + 1];
                webTarget = webTarget.queryParam(key, value);
            }
        }
        long timestamp = System.currentTimeMillis();
        Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON)
        		.header(HEADER_API_KEY, publicKey)
        		.header(HEADER_API_SIGN, sign(timestamp, "GET", webTarget.getUri().toString().replaceAll(API_URL, ""), null))
        		.header(HEADER_API_TIMESTAMP, timestamp)
        		.header(HEADER_API_PASSPHRASE, sign(passphrase))
        		.header(HEADER_API_VERSION, version);
        try (Response response = builder.get()) {
        	response.bufferEntity();
            if (response.getStatus() == 200) {
            	try {
            		return response.readEntity(type);
            	} catch (ProcessingException e) {
                    throw new RuntimeException("SecuredKucoinAPI> Cannot deserialize " + webTarget.toString() + " : " + response.readEntity(String.class));
            	}
            } else {
                throw new RuntimeException("SecuredKucoinAPI> HTTP response code " + response.getStatus() + " from "
                        + webTarget.toString() + " : " + response.readEntity(String.class));
            }
        }
	}

	public <T> T post(String path, GenericType<T> type, String body) {
		WebTarget webTarget = client.target(API_URL).path(path);
		long timestamp = System.currentTimeMillis();
		Invocation.Builder builder = webTarget.request("application/json")
        		.header(HEADER_API_KEY, publicKey)
        		.header(HEADER_API_SIGN, sign(timestamp, "POST", webTarget.getUri().toString().replaceAll(API_URL, ""), body))
        		.header(HEADER_API_TIMESTAMP, timestamp)
        		.header(HEADER_API_PASSPHRASE, sign(passphrase))
        		.header(HEADER_API_VERSION, version);
		try (Response response = builder.post(Entity.entity(body, "application/json"))) {
			response.bufferEntity();
			if (response.getStatus() == 200) {
				try {
            		return response.readEntity(type);
            	} catch (ProcessingException e) {
                    throw new RuntimeException("SecuredKucoinAPI> Cannot deserialize " + webTarget.toString() + " " + body + ": " + response.readEntity(String.class));
            	}
			} else {
                throw new RuntimeException("SecuredKucoinAPI> HTTP response code " + response.getStatus() + " from "
                        + webTarget.toString() + " " + body + ": " + response.readEntity(String.class));
			}
		}
	}

	private String sign(long timestamp, String method, String endpoint, String body) {
		StringBuilder builder = new StringBuilder().append(timestamp).append(method).append(endpoint);
		if (body != null) {
			builder.append(body);
		}
		return sign(builder.toString());
	}
	
	private String sign(String data) {
		return new String(Base64.getEncoder().encode(mac.doFinal(data.getBytes())));
	}

	@Override
	public Map<String, String> wallet() {
		Map<String, String> wallet = new HashMap<>();
		KucoinResponse<List<KucoinAccount>> response = get("/api/v1/accounts/", new GenericType<KucoinResponse<List<KucoinAccount>>>() {}, "type", "trade");
		response.getData().stream().forEach(item -> wallet.put(item.getCurrency(), item.getAvailable()));
		return wallet;
	}
	
	public Map<String, String> getOrder(String orderId){
		return get("/api/v1/orders/" + orderId, new GenericType<KucoinResponse<Map<String, String>>>() {}).getData();
	}

	@Override
	public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice) {
		int rounded = new BigDecimal(quoteOrderQty).intValue();
		String orderId = order(symbol, action, BuySell.funds, Integer.toString(rounded));
		CsvTransactionRow tx = Utils.calculatedUsdtCsvTransactionRow(new Date(), symbol, orderId, action, Integer.toString(rounded), currentUsdtPrice, buyCommission);
		return tx;
	}

	@Override
	public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice) {
		SymbolLimits limits = getSymbolLimits(symbol);
		String fixedQuantity = Utils.filterLotSizeQuantity(quantity, limits.baseMinSize, limits.baseMaxSize, limits.baseIncrement);
		String orderId = order(symbol, action, BuySell.size, fixedQuantity);
		CsvTransactionRow tx = Utils.calculatedSymbolCsvTransactionRow(new Date(), symbol, orderId, action, fixedQuantity, currentUsdtPrice, sellCommission);
		return tx;
	}
	
	private String order(String symbol, Action action, BuySell key, String value) {
		String kucoinSymbol = symbol.replaceFirst(Utils.USDT, "-" + Utils.USDT);
		String side = action.side().toLowerCase();
		String clientOid = UUID.randomUUID().toString();
		StringBuilder body = new StringBuilder("{");
		body.append("\"clientOid\":").append("\"").append(clientOid).append("\",");
		body.append("\"side\":").append("\"").append(side).append("\",");
		body.append("\"symbol\":").append("\"").append(kucoinSymbol).append("\",");
		body.append("\"type\":").append("\"").append("market").append("\",");
		body.append("\""+ key.name() +"\":").append("\"").append(value).append("\",");
		body.append("\"symbol\":").append("\"").append(kucoinSymbol).append("\"");
		body.append("}");
		LOGGER.info(() -> "SecuredKucoinAPI> Prepared order: " + body.toString());
		Map<String, String> response = post("/api/v1/orders", new GenericType<KucoinResponse<Map<String, String>>>() {}, body.toString()).getData();
		String orderId = response.get("orderId");
		Map<String, String> orderInfo = getOrder(orderId);
		LOGGER.info(() -> "SecuredKucoinAPI> Order Info: " + orderInfo + ", " + response);
		/* FIXME Sometimes dealFunds and dealSize comes much lower than it should be, for example: side=SELL&quantity=45459.00811599 -> 
		* Order Info: {id=xxxxxx, symbol=AI-USDT, opType=DEAL, type=market, side=sell, price=0, size=45459.0081, funds=0, dealFunds=259.209449966, 
		* dealSize=11166.3079, fee=0.518418899932, feeCurrency=USDT, stp=, stop=, stopTriggered=false, stopPrice=0, timeInForce=GTC, postOnly=false, 
		* hidden=false, iceberg=false, visibleSize=0, cancelAfter=0, channel=API, clientOid=373fd711-ceee-49db-96a4-575dfe305ac3, remark=null, tags=null, 
		* isActive=true, cancelExist=false, createdAt=1640789169018, tradeType=TRADE}
		* 
		* This is example of buy:
		* {id=xxxxxx, symbol=POLX-USDT, opType=DEAL, type=market, side=buy, price=0, size=0, funds=200, dealFunds=0, dealSize=0,
		* fee=0, feeCurrency=USDT, stp=, stop=, stopTriggered=false, stopPrice=0, timeInForce=GTC, postOnly=false, hidden=false,
		* iceberg=false, visibleSize=0, cancelAfter=0, channel=API, clientOid=3b93210b-c2b3-42a3-9c9b-937b212e89ed, remark=null,
		* tags=null, isActive=true, cancelExist=false, createdAt=1641423626808, tradeType=TRADE}
		* 
		* {id=xxxxxx, symbol=POLX-USDT, opType=DEAL, type=market, side=buy, price=0, size=0, funds=200, dealFunds=199.9999999999942037,
		* dealSize=289140.80359094, fee=0.3999999999999884074, feeCurrency=USDT, stp=, stop=, stopTriggered=false, stopPrice=0, timeInForce=GTC, postOnly=false,
		* hidden=false, iceberg=false, visibleSize=0, cancelAfter=0, channel=API, clientOid=eacfd6b6-a274-45c2-9482-79d8e71afa20, remark=null, tags=null,
		* isActive=true, cancelExist=false, createdAt=1641420043454, tradeType=TRADE}
		*/
		return orderId;
	}
	
	private static enum BuySell {
	    // Size is quantity of symbol, and funds is quantity of USD
	    size, funds;
	}
	
	public SymbolLimits getSymbolLimits(String symbol) {
		String kukoinSymbol = symbol.replaceFirst(Utils.USDT, "-" + Utils.USDT);
		List<Map<String, String>> data = get("/api/v1/symbols", new GenericType<KucoinResponse<List<Map<String, String>>>>() {}).getData();
		return data.stream().filter(d -> kukoinSymbol.equals(d.get("symbol")))
				.map(d -> new SymbolLimits(d.get("baseMinSize"), d.get("quoteMinSize"), d.get("baseMaxSize"), d.get("quoteMaxSize"), d.get("baseIncrement"), d.get("quoteIncrement"), d.get("baseCurrency"), d.get("quoteCurrency")))
				.findAny().get();
	}
	
	private Map<String, Object> withdrawalQuotas(String symbol) {
		String coin = symbol.replaceFirst(Utils.USDT, "");
		return get("/api/v1/withdrawals/quotas", new GenericType<KucoinResponse<Map<String, Object>>>() {}, "currency", coin).getData();
	}
	
	public Deposit depositAddress(String symbol, Map<String, Double> sellPrices) {
		double usdUnit = sellPrices.get(symbol);
		String coin = symbol.replaceFirst(Utils.USDT, "");
		Map<String, Object> quotas = withdrawalQuotas(symbol);
		Deposit deposit = new Deposit(coin, (Boolean) quotas.get("isWithdrawEnabled"), (String) quotas.get("withdrawMinFee"), (String) quotas.get("remainAmount"), (String) quotas.get("withdrawMinSize"), (String) quotas.get("chain"));
		deposit.setRemainAmountUsd(Utils.format(Utils.usdValue(Double.parseDouble(deposit.getRemainAmount()), usdUnit)));
		deposit.setWithdrawMinFeeUsd(Utils.format(Utils.usdValue(Double.parseDouble(deposit.getWithdrawMinFee()), usdUnit)));
		deposit.setWithdrawMinSizeUsd(Utils.format(Utils.usdValue(Double.parseDouble(deposit.getWithdrawMinSize()), usdUnit)));
		deposit.setUsdUnit(Utils.format(usdUnit));
		if (deposit.isWithdrawEnabled()) {
			Map<String, String> response = get("/api/v1/deposit-addresses", new GenericType<KucoinResponse<Map<String, String>>>() {}, "currency", coin).getData();
			LOGGER.info("SecuredKucoinAPI> " + coin + " address: " + response);
			if (response == null) {
				StringBuilder body = new StringBuilder("{");
				body.append("\"currency\":").append("\"").append(coin).append("\"");
				body.append("}");
				response = post("/api/v1/deposit-addresses", new GenericType<KucoinResponse<Map<String, String>>>() {}, body.toString()).getData();
			}
			deposit.setAddress(response.get("address"));
		}
		return deposit;
	}
	
	public String applyWithdraw(String remoteAddress, double amount, Deposit info) {
		info.calculate(amount);
		StringBuilder body = new StringBuilder("{");
		body.append("\"currency\":").append("\"").append(info.getCurrency()).append("\"").append(",");
		body.append("\"amount\":").append(Utils.format(amount)).append(",");
		body.append("\"isInner\":").append("false").append(",");
		body.append("\"memo\":").append("\"").append(info.getMemo()).append("\"").append(",");
		body.append("}");
		Map<String, String> response = post("/api/v1/withdrawals", new GenericType<KucoinResponse<Map<String, String>>>() {}, body.toString()).getData();
		return response.get("withdrawalId");
	}
	
	public static SecuredKucoinAPI create(PropertiesKucoin cloudProperties, Client client) throws InvalidKeyException, NoSuchAlgorithmException {
		return new SecuredKucoinAPI(client, cloudProperties.kucoinPublicKey(), cloudProperties.kucoinPrivateKey(), cloudProperties.kucoinApiPassPhrase(), cloudProperties.kucoinApiVersion(), cloudProperties.kucoinBuyCommission(), cloudProperties.kucoinSellCommission());
	}
	
	private static class SymbolLimits {
		private final String baseMinSize;
		private final String quoteMinSize;
		private final String baseMaxSize;
		private final String quoteMaxSize;
		private final String baseIncrement;
		private final String quoteIncrement;
		private final String baseCurrency;
		private final String quoteCurrency;
		public SymbolLimits(String baseMinSize, String quoteMinSize, String baseMaxSize, String quoteMaxSize,
				String baseIncrement, String quoteIncrement, String baseCurrency, String quoteCurrency) {
			this.baseMinSize = baseMinSize;
			this.quoteMinSize = quoteMinSize;
			this.baseMaxSize = baseMaxSize;
			this.quoteMaxSize = quoteMaxSize;
			this.baseIncrement = baseIncrement;
			this.quoteIncrement = quoteIncrement;
			this.baseCurrency = baseCurrency;
			this.quoteCurrency = quoteCurrency;
		}
		@Override
		public String toString() {
			return "SymbolLimits [baseMinSize=" + baseMinSize + ", quoteMinSize=" + quoteMinSize + ", baseMaxSize="
					+ baseMaxSize + ", quoteMaxSize=" + quoteMaxSize + ", baseIncrement=" + baseIncrement
					+ ", quoteIncrement=" + quoteIncrement + ", baseCurrency=" + baseCurrency + ", quoteCurrency="
					+ quoteCurrency + "]";
		}
		
	}
	
	public static class KucoinResponse<T> {
		private String code;
		private T data;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public T getData() {
			return data;
		}
		public void setData(T data) {
			this.data = data;
		}
		@Override
		public String toString() {
			return "KucoinResponse [code=" + code + ", data=" + data + "]";
		}
	}
	
	public static class KucoinAccount {
		private String id;
		private String currency;
		private String type;
		private String balance;
		private String available;
		private String holds;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getCurrency() {
			return currency;
		}
		public void setCurrency(String currency) {
			this.currency = currency;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getBalance() {
			return balance;
		}
		public void setBalance(String balance) {
			this.balance = balance;
		}
		public String getAvailable() {
			return available;
		}
		public void setAvailable(String available) {
			this.available = available;
		}
		public String getHolds() {
			return holds;
		}
		public void setHolds(String holds) {
			this.holds = holds;
		}
	}

	@Override
	public Client getClient() {
		return client;
	}
}
