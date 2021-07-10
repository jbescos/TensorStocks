package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
        ExchangeInfo info = BinanceAPI.exchangeInfo(symbol);
        Map<String, Object> filter = info.getFilter(symbol, ExchangeInfo.LOT_SIZE);
        assertEquals("1.00", filter.get("minQty").toString());
        assertEquals("10000000000.00", filter.get("maxQty").toString());
        assertEquals("1.00", filter.get("stepSize").toString());
    }
    
    @Test
    @Ignore
    public void klines() {
    	Calendar now = Calendar.getInstance();
    	now.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY) - 1);
    	now.set(Calendar.MINUTE, 0);
    	long startTime = now.getTime().getTime();
    	now.set(Calendar.MINUTE, 30);
    	long endTime = now.getTime().getTime();
    	String symbol = "BTCUSDT";
    	List<Kline> klines = BinanceAPI.klines(Interval.MINUTES_30, symbol, 1, startTime, endTime);
    	LOGGER.info(klines.toString());
    }
}
