package com.jbescos.exchange;

public class Price {

    private final String symbol;
    private final double price;
    private final String token;

    public Price(String symbol, double price, String token) {
        this.symbol = symbol;
        this.price = price;
        this.token = token;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return Double.toString(price);
    }

}
