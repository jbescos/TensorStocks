package com.jbescos.test;

import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.SecuredArkenAPI;
import com.jbescos.exchange.Broker.Action;

public class SecuredArkenAPITest {

    private final String USER_NAME = "XXXXXXXXX";
    private final String API_TOKEN = "XXXXXXXXX";
    private final String CHAIN = "bsc";
    private final String POOL_ADDRESS = "XXXXXXXXX";
    private final String PRIVATE_KEY = "XXXXXXXXX";
    private final SecuredArkenAPI api = new SecuredArkenAPI(ClientBuilder.newClient(), USER_NAME, API_TOKEN, CHAIN, POOL_ADDRESS, PRIVATE_KEY);
    
    @Test
    @Ignore
    public void price() {
        System.out.println(api.price());      
    }
    
    @Test
    @Ignore
    public void pools() {
        System.out.println(api.pool());        
    }
    
    @Test
    @Ignore
    public void chains() {
        System.out.println(api.chains());        
    }
    
    @Test
    @Ignore
    public void buy() {
        System.out.println(api.orderUSDT("binance-bitcoin", Action.BUY, "0.00001", 0));        
    }
    
}
