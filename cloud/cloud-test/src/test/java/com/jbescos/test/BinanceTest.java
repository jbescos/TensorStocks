package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.exchange.Kline;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.Utils;
import com.jbescos.exchange.PublicAPI.Interval;

public class BinanceTest {

    private static final Logger LOGGER = Logger.getLogger(BinanceTest.class.getName());

    @Test
    @Ignore
    public void exchange() {
//      {filterType=LOT_SIZE, minQty=1.00, maxQty=46116860414.00, stepSize=1.00}
        String symbol = "SHIBUSDT";
        Client client = ClientBuilder.newClient();
        Map<String, Object> filter = new PublicAPI(client).exchangeInfoFilter(symbol, "LOT_SIZE");
        System.out.println(filter);
        assertEquals("1.00", filter.get("minQty").toString());
        assertEquals("46116860414.00", filter.get("maxQty").toString());
        assertEquals("1.00", filter.get("stepSize").toString());
        client.close();
    }

    @Test
    @Ignore
    public void klines() {
        Date now = new Date();
        Date mins15Earlier = new Date(now.getTime() - ((1000 * 60 * 15) + 80000));
        Interval interval = Interval.getInterval(mins15Earlier.getTime(), now.getTime());
        assertEquals(Interval.MINUTES_15, interval);
        long from = interval.from(mins15Earlier.getTime());
        long to = interval.to(from);
        String symbol = "BTCUSDT";
        Client client = ClientBuilder.newClient();
        List<Kline> klines = new PublicAPI(client).klines(interval, symbol, null, from, to);
        LOGGER.info(() -> klines.toString());
        Kline kline = klines.get(0);
        LOGGER.info(() -> "Asking for data between " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(from)) + " to "
                + Utils.fromDate(Utils.FORMAT_SECOND, new Date(to)) + " and received from "
                + Utils.fromDate(Utils.FORMAT_SECOND, new Date(kline.getOpenTime())) + " to "
                + Utils.fromDate(Utils.FORMAT_SECOND, new Date(kline.getCloseTime())));
        client.close();
    }

    @Test
    @Ignore
    public void serverTime() {
        Client client = ClientBuilder.newClient();
        long time = new PublicAPI(client).time();
        LOGGER.info(() -> "Server time is: " + Utils.fromDate(Utils.FORMAT_SECOND, new Date(time)));
    }
}
