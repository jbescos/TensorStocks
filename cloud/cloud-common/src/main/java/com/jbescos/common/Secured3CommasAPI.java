package com.jbescos.common;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;

//https://github.com/3commas-io/3commas-official-api-docs/tree/master
//Buy
//{
//    "account_id": 1,
//    "pair": "USDT_BTC",
//    "instant": "true",
//    "position": {
//        "type": "buy",
//        "units": {
//            "value": "0.01"
//        },
//        "order_type": "market"
//    }
//}
//Sell
//{
//    "account_id": 1,
//    "pair": "USDT_BTC",
//    "instant": "true",
//    "position": {
//        "type": "sell",
//        "units": {
//            "value": "0.01"
//        },
//        "order_type": "market"
//    }
//}
//Response
//{
//    "id": XXXXXX,
//    "version": 2,
//    "account": {
//            "id": XXXXXX,
//            "type": "kucoin",
//            "name": "My Kucoin",
//            "market": "Kucoin Spot",
//            "link": "/accounts/XXXXXX"
//    },
//    "pair": "USDT_SUSHI",
//    "instant": true,
//    "status": {
//            "type": "created",
//            "basic_type": "created",
//            "title": "Pending"
//    },
//    "leverage": {
//            "enabled": false
//    },
//    "position": {
//            "type": "buy",
//            "editable": false,
//            "units": {
//                    "value": "5.0",
//                    "editable": false
//            },
//            "price": {
//                    "value": "1.4918",
//                    "value_without_commission": "1.4918",
//                    "editable": true
//            },
//            "total": {
//                    "value": "7.459"
//            },
//            "order_type": "market",
//            "status": {
//                    "type": "to_process",
//                    "basic_type": "to_process",
//                    "title": "Pending"
//            }
//    },
//    "take_profit": {
//            "enabled": false,
//            "price_type": "value",
//            "steps": []
//    },
//    "stop_loss": {
//            "enabled": false
//    },
//    "reduce_funds": {
//            "steps": []
//    },
//    "market_close": {},
//    "note": "",
//    "note_raw": null,
//    "skip_enter_step": false,
//    "data": {
//            "editable": false,
//            "current_price": {
//                    "bid": "1.4917",
//                    "ask": "1.4918",
//                    "last": "1.4918",
//                    "quote_volume": "236709.50620738",
//                    "day_change_percent": "-5.31"
//            },
//            "target_price_type": "price",
//            "orderbook_price_currency": "USDT",
//            "base_order_finished": true,
//            "missing_funds_to_close": "0.0",
//            "liquidation_price": null,
//            "average_enter_price": null,
//            "average_close_price": null,
//            "average_enter_price_without_commission": null,
//            "average_close_price_without_commission": null,
//            "panic_sell_available": false,
//            "add_funds_available": false,
//            "reduce_funds_available": false,
//            "force_start_available": true,
//            "force_process_available": true,
//            "cancel_available": false,
//            "finished": false,
//            "base_position_step_finished": false,
//            "entered_amount": "0.0",
//            "entered_total": "0.0",
//            "closed_amount": "0.0",
//            "closed_total": "0.0",
//            "commission": "0.001",
//            "created_at": "2024-04-05T11:51:46.975Z",
//            "updated_at": "2024-04-05T11:51:46.975Z",
//            "type": "simple_buy"
//    },
//    "profit": {
//            "volume": null,
//            "usd": null,
//            "percent": "0.0",
//            "roe": null
//    },
//    "margin": {
//            "amount": null,
//            "total": null
//    },
//    "is_position_not_filled": true
//}
public class Secured3CommasAPI implements SecuredAPI {

    private static final Logger LOGGER = Logger.getLogger(Secured3CommasAPI.class.getName());
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String DEFAULT_WALLET_CONTENT = "1000000000";
    private final Map<String, String> wallet = new HashMap<>();
    private static final String URL = "https://api.3commas.io";
    private static final String HEADER_API_KEY = "Apikey";
    private static final String HEADER_SECRET = "Signature";
    private final String publicKey;
    private final String accountId;
    private final Mac mac;
    private final Client client;

    private Secured3CommasAPI(String accountId, String publicKey, String privateKey, List<String> whitelist, Client client) throws NoSuchAlgorithmException, InvalidKeyException {
        this.accountId = accountId;
        this.publicKey = publicKey;
        this.mac = Mac.getInstance(HMAC_SHA_256);
        mac.init(new SecretKeySpec(privateKey.getBytes(), HMAC_SHA_256));
        this.client = client;
        wallet.put(Utils.USDT, DEFAULT_WALLET_CONTENT);
        for (String symbol : whitelist) {
            String walletSymbol = symbol.replaceFirst(Utils.USDT, "");
            wallet.put(walletSymbol, DEFAULT_WALLET_CONTENT);
        }
    }

    public String sign(String endpoint, String body) {
        StringBuilder builder = new StringBuilder().append(endpoint);
        if (body != null) {
            builder.append(body);
        }
        return sign(builder.toString());
    }

    private String sign(String data) {
        byte[] signature = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder(signature.length * 2);
        for(byte b: signature) {
           sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public Map<String, String> wallet() {
        return wallet;
    }

    private String json(String pair, String type, String value) {
        StringBuilder body = new StringBuilder("{");
        body.append("\"account_id\":").append(accountId).append(",");
        body.append("\"pair\":").append("\"").append(pair).append("\",");
        body.append("\"instant\":").append("\"").append("true").append("\",");
        body.append("\"position\":{");
        body.append("\"type\":").append("\"").append(type).append("\",");
        body.append("\"units\":{");
        body.append("\"value\":").append("\"").append(value).append("\"");
        body.append("},");
        body.append("\"order_type\":").append("\"").append("market").append("\"");
        body.append("}");
        body.append("}");
        return body.toString();
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice,
            boolean hasPreviousTransactions) {
        // 3commas only support symbol
        String quantity = Utils.format(Utils.symbolValue(Double.parseDouble(quoteOrderQty), currentUsdtPrice));
        return orderSymbol(symbol, action, quantity, currentUsdtPrice, hasPreviousTransactions);
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice,
            boolean hasPreviousTransactions) {
        String asset = symbol.replaceFirst(Utils.USDT, "");
        String body = json(Utils.USDT + "_" + asset, action.side().toLowerCase(), quantity);
        Map<String, Object> response = post("/public/api/v2/smart_trades", new GenericType<Map<String, Object>>() {}, body);
        LOGGER.info("Secured3CommasAPI order Symbol response: " + response);
        return null;
    }

    public <T> T post(String path, GenericType<T> type, String body) {
        WebTarget webTarget = client.target(URL).path(path);
        Invocation.Builder builder = webTarget.request("application/json").header(HEADER_API_KEY, publicKey)
                .header(HEADER_SECRET, sign(webTarget.getUri().toString().replaceAll(URL, ""), body));
        try (Response response = builder.post(Entity.entity(body, "application/json"))) {
            response.bufferEntity();
            if (response.getStatus() == 200 || response.getStatus() == 201) {
                try {
                    return response.readEntity(type);
                } catch (ProcessingException e) {
                    throw new RuntimeException("Secured3CommasAPI> Cannot deserialize " + webTarget.toString() + " "
                            + body + ": " + response.readEntity(String.class));
                }
            } else {
                throw new RuntimeException("Secured3CommasAPI> HTTP response code " + response.getStatus() + " from "
                        + webTarget.toString() + " " + body + ": " + response.readEntity(String.class));
            }
        }
    }

    @Override
    public CsvTransactionRow synchronize(CsvTransactionRow precalculated) {
        throw new IllegalStateException("3Commas does not support synchornize transactions");
    }

    public static Secured3CommasAPI create(String accountId, String publicKey, String privateKey, List<String> whitelist, Client client)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return new Secured3CommasAPI(accountId, publicKey, privateKey, whitelist, client);
    }

    public static Secured3CommasAPI create(CloudProperties cloudProperties, Client client)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return create(cloudProperties.COMMAS_ACCOUNT_ID, cloudProperties.COMMAS_PUBLIC_KEY, cloudProperties.COMMAS_PRIVATE_KEY, cloudProperties.BOT_WHITE_LIST_SYMBOLS, client);
    }

}
