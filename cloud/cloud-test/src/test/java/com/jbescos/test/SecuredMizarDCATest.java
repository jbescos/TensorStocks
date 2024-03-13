package com.jbescos.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.SecuredMizarDCA;
import org.junit.Ignore;
import org.junit.Test;

public class SecuredMizarDCATest {

    private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties(null);
    private final Client client = ClientBuilder.newClient();
    private final SecuredMizarDCA mizarApi = SecuredMizarDCA.create(CLOUD_PROPERTIES, client);

    @Test
    @Ignore
    public void openPosition() {
        mizarApi.openPosition("UNFI", "USDT", 0.1);
    }

    @Test
    @Ignore
    public void closePosition() {
        mizarApi.closeAllBySymbol("UNFI");
    }

}
