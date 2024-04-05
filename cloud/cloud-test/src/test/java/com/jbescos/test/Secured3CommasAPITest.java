package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.Secured3CommasAPI;
import com.jbescos.exchange.Broker.Action;
import org.junit.Ignore;
import org.junit.Test;

public class Secured3CommasAPITest {

    private static final Client client = ClientBuilder.newClient();
    private final CloudProperties cloudProperties = new CloudProperties(null);

    @Test
    public void signature() throws InvalidKeyException, NoSuchAlgorithmException {
        Secured3CommasAPI api = Secured3CommasAPI.create("", "vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A", "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j", Collections.emptyList(), client);
        String sign = api.sign("/public/api/ver1/users/change_mode?mode=paper", null);
        assertEquals("bca8d8c10acfbe8e76c5335d3efbe0a550487170a8bb7aaea0a13efabab55316", sign);
    }

    @Test
    @Ignore
    public void buyUSDT() throws InvalidKeyException, NoSuchAlgorithmException {
        Secured3CommasAPI api = Secured3CommasAPI.create(cloudProperties, client);
        // It is mandatory that currentUsdtPrice is correct, because at the end it invokes orderSymbol
        api.orderUSDT("SUSHIUSDT", Action.BUY, "10", 1.4932, false);
    }

    @Test
    @Ignore
    public void sellSymbol() throws InvalidKeyException, NoSuchAlgorithmException {
        Secured3CommasAPI api = Secured3CommasAPI.create(cloudProperties, client);
        api.orderSymbol("SUSHIUSDT", Action.SELL, "11.697", 1.4932, false);
    }
}
