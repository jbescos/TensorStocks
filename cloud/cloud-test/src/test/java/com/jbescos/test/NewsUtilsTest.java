package com.jbescos.test;

import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.FileStorage;
import com.jbescos.common.NewsUtils;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.News;

public class NewsUtilsTest {

    private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties(null);

    @Test
    @Ignore
    public void news() {
        long millis = 1661178302000L;
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        Map<Exchange, List<News>> newsExchanges = NewsUtils.news(millis, publicAPI, CLOUD_PROPERTIES, client);
        String temp = System.getProperty("java.io.tmpdir");
        FileStorage storage = new FileStorage(temp);
        NewsUtils.saveNews(storage, newsExchanges);
        List<String> delisted = NewsUtils.delisted(storage, Exchange.KUCOIN);
        System.out.println(temp + " -> " + delisted);
    }
}
