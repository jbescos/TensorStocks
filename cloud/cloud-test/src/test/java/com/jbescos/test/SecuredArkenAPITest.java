package com.jbescos.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.SecuredArkenAPI;

public class SecuredArkenAPITest {

    private final String USER_NAME = "XXXXXXXXXXXXX";
    private final String API_TOKEN = "XXXXXXXXXXXXX";
    private final String CHAIN = "ethereum";
    private final String POOL_ADDRESS = "XXXXXXXXXXXXX";
    
    @Test
    @Ignore
    public void price() {
        Client client = ClientBuilder.newClient();
        SecuredArkenAPI api = new SecuredArkenAPI(client, USER_NAME, API_TOKEN, CHAIN, POOL_ADDRESS);
        System.out.println(api.price("addresses", "0x29d578cec46b50fa5c88a99c6a4b70184c062953,0xf293d23bf2cdc05411ca0eddd588eb1977e8dcd4"));      
    }
    
    @Test
    @Ignore
    public void pools() {
        Client client = ClientBuilder.newClient();
        SecuredArkenAPI api = new SecuredArkenAPI(client, USER_NAME, API_TOKEN, CHAIN, POOL_ADDRESS);
        System.out.println(api.pool());        
    }
    
    @Test
    @Ignore
    public void chains() {
        Client client = ClientBuilder.newClient();
        SecuredArkenAPI api = new SecuredArkenAPI(client, USER_NAME, API_TOKEN, CHAIN, POOL_ADDRESS);
        System.out.println(api.chains());        
    }
    
}
