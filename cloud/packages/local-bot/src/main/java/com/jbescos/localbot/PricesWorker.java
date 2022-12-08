package com.jbescos.localbot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PricesWorker<T extends Priceable> implements MessageWorker<T> {

    private final AtomicBoolean notWorking = new AtomicBoolean(true);
    private final String symbol;
    private final ConcurrentHashMap<String, Price> prices;

    public PricesWorker(String symbol, ConcurrentHashMap<String, Price> prices) {
        this.symbol = symbol;
        this.prices = prices;
    }

    @Override
    public boolean startToWork() {
        return notWorking.compareAndSet(true, false);
    }

    @Override
    public void process(T message, long now) {
        try {
            Price price = message.toPrice();
            price.setTimestamp(now);
            prices.put(symbol, price);
        } finally {
            notWorking.set(true);
        }
    }

    public static class Price {
        private final String buyPrice;
        private final String sellPrice;
        private final String symbol;
        private long timestamp;

        public Price(String buyPrice, String sellPrice, String symbol) {
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.symbol = symbol;
        }

        public String getBuyPrice() {
            return buyPrice;
        }

        public String getSellPrice() {
            return sellPrice;
        }

        public String getSymbol() {
            return symbol;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

    }
}
