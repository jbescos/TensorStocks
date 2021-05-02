package es.tododev.stocks.binance;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class BinanceAPITest {

	@Test
	public void signature() throws InvalidKeyException, NoSuchAlgorithmException {
		BinanceAPI api = BinanceAPI.create("vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A", "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j");
		String signature = api.signature("symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559");
		assertEquals("c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71", signature);
	}
}
