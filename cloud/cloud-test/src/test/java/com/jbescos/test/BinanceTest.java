package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BinanceAPI.Interval;
import com.jbescos.common.ExchangeInfo;
import com.jbescos.common.Kline;

public class BinanceTest {

	private static final Logger LOGGER = Logger.getLogger(BinanceTest.class.getName());

    @Test
    @Ignore
    public void exchange() {
//        "filterType": "LOT_SIZE",
//        "minQty": "1.00",
//        "maxQty": "10000000000.00",
//        "stepSize": "1.00"
        String symbol = "SHIBUSDT";
        Client client = ClientBuilder.newClient();
        ExchangeInfo info = new BinanceAPI(client).exchangeInfo(symbol);
        Map<String, Object> filter = info.getFilter(symbol, ExchangeInfo.LOT_SIZE);
        assertEquals("1.00", filter.get("minQty").toString());
        assertEquals("10000000000.00", filter.get("maxQty").toString());
        assertEquals("1.00", filter.get("stepSize").toString());
        client.close();
    }
    
    @Test
    @Ignore
    public void klines() {
    	Calendar now = Calendar.getInstance();
    	now.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY) - 1);
    	now.set(Calendar.MINUTE, 0);
    	long startTime = now.getTime().getTime();
    	String symbol = "BTCUSDT";
    	Client client = ClientBuilder.newClient();
    	List<Kline> klines = new BinanceAPI(client).klines(Interval.MINUTES_30, symbol, null, startTime, null);
    	LOGGER.info(klines.toString());
    	client.close();
    }
}
