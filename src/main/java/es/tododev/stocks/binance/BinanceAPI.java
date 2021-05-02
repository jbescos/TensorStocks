package es.tododev.stocks.binance;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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

import org.glassfish.jersey.client.ClientConfig;

public final class BinanceAPI implements OperatorAPI {

	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	private static final String HMAC_SHA_256 = "HmacSHA256";
	private static final String URL = "https://api.binance.com";
	private final Mac mac;
	private final String publicKey;

	private BinanceAPI(String publicKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
		mac = Mac.getInstance(HMAC_SHA_256);
		mac.init(new SecretKeySpec(privateKey.getBytes(), HMAC_SHA_256));
		this.publicKey = publicKey;
	}

	synchronized String signature(String data) {
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
	
    @Override
	public <T> T request(String path, String query, boolean signed, boolean post, GenericType<T> type) {
		Client client = ClientBuilder.newClient(new ClientConfig());
		if (signed && query != null) {
			String signature = signature(query);
			query = query + "&signature=" + signature;
		}
		if (query != null) {
			path = path + "?" + query;
		}
		WebTarget webTarget = client.target(URL).path(path);
		Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON).header("X-MBX-APIKEY", publicKey);
		try (Response response = post ? builder.post(Entity.text("")) : builder.get()) {
			if (response.getStatus() == 200) {
				return response.readEntity(type);
			} else {
				response.bufferEntity();
				throw new RuntimeException("HTTP response code " + response.getStatus() + " from " + webTarget.getUri().toString() + " : " + response.readEntity(String.class));
			}
		}
	}
	
	public static BinanceAPI create(String publicKey, String privateKey) throws InvalidKeyException, NoSuchAlgorithmException {
		return new BinanceAPI(publicKey, privateKey);
	}

}
