package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.BinanceAPI;
import com.jbescos.common.ExchangeInfo;

public class BinanceTest {

    @Test
    @Ignore
    public void exchange() {
//        "filterType": "LOT_SIZE",
//        "minQty": "1.00",
//        "maxQty": "10000000000.00",
//        "stepSize": "1.00"
        String symbol = "SHIBUSDT";
        ExchangeInfo info = BinanceAPI.exchangeInfo(symbol);
        Map<String, Object> filter = info.getFilter(symbol, ExchangeInfo.LOT_SIZE);
        assertEquals("1.00", filter.get("minQty").toString());
        assertEquals("10000000000.00", filter.get("maxQty").toString());
        assertEquals("1.00", filter.get("stepSize").toString());
    }
}
