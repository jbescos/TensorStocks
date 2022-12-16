package com.jbescos.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.cloudchart.CandleChart;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.FearGreedIndex;
import com.jbescos.exchange.Kline;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.Interval;
import com.jbescos.exchange.Utils;

public class PublicAPITest {

    @Test
    @Ignore
    public void simplePrices() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        System.out.println(Utils.simplePrices(Exchange.KUCOIN.price(publicAPI)));
        client.close();
    }

    @Test
    @Ignore
    public void allPrices() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        Set<String> updatedExchanges = new HashSet<>();
        for (Exchange exchange : Exchange.values()) {
            if (exchange.enabled()) {
                if (updatedExchanges.add(exchange.getFolder())) {
                    System.out.println(exchange.name() + ": " + exchange.price(publicAPI));
                }
            }
        }
        client.close();
    }

    @Test
    @Ignore
    public void getFearGreedIndex() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        List<FearGreedIndex> fearGreed = publicAPI.getFearGreedIndex("2");
        System.out.println(fearGreed);
        client.close();
    }

    @Test
    @Ignore
    public void klines() throws FileNotFoundException, IOException {
        int limit = 150;
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        Date from = Utils.getDateOfDaysBackZero(new Date(), limit);
        List<Kline> klines = publicAPI.klines(Interval.DAY_1, "BTCUSDT", limit, from.getTime(), null);
        System.out.println("Data from " + from.getTime() + ":" + klines);
        client.close();
        File chartFile = new File("./target/klines.png");
        try (FileOutputStream output = new FileOutputStream(chartFile)) {
            CandleChart chart = new CandleChart();
            chart.add("BTCUSDT", klines);
            chart.save(output, "BTC KLines", "", "USDT");
        }
    }

    @Test
    @Ignore
    public void compatibleSymbols() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        Map<String, Price> pricesKucoin = publicAPI.priceKucoin();
        Map<String, Price> pricesBinance = publicAPI.priceBinance();
        Set<String> compatibleSymbols = new HashSet<>();
        for (String symbol : pricesBinance.keySet()) {
            if (pricesKucoin.containsKey(symbol)) {
                compatibleSymbols.add(symbol);
                if (compatibleSymbols.size() == 100) {
                    break;
                }
            }
        }
        client.close();
        System.out.println("Compatible symbols: " + compatibleSymbols);
    }

    @Test
    @Ignore
    public void prices() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        Map<String, Price> binancePrices = publicAPI.priceBinance();
        Map<String, Price> kucoinSellPrices = publicAPI.priceKucoin();
        Map<String, Price> kucoinBuyPrices = publicAPI.priceKucoin(ticker -> Double.parseDouble(ticker.getSell()));
        for (String symbol : new CloudProperties("binance", null).BOT_WHITE_LIST_SYMBOLS) {
            Double binanceSellPrice = binancePrices.get(symbol).getPrice();
            Double kucoinSellPrice = kucoinSellPrices.get(symbol).getPrice();
            Double kucoinBuyPrice = kucoinBuyPrices.get(symbol).getPrice();
            if (binanceSellPrice != null)
                System.out.println("Binance price: " + Utils.format(binanceSellPrice) + " " + symbol);
            if (kucoinBuyPrice != null)
                System.out.println("Kucoin buy price: " + Utils.format(kucoinBuyPrice) + " " + symbol);
            if (kucoinSellPrice != null)
                System.out.println("Kucoin sell price: " + Utils.format(kucoinSellPrice) + " " + symbol);
        }
        client.close();
    }

    @Test
    @Ignore
    public void priceCoingecko() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        System.out.println(publicAPI.priceCoingecko("ethereum"));
        client.close();
    }

    @Test
    @Ignore
    public void priceCoingeckoTop() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        System.out.println(publicAPI.priceCoingeckoTop(5, "ethereum"));
        client.close();
    }

    @Test
    @Ignore
    public void kucoinNews() {
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        System.out.println(publicAPI.delistedKucoin(1661178302000L));
        client.close();
    }
}
