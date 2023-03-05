package com.jbescos.exchange;

import java.util.Date;

public class CsvWalletRow {

    public static final String HEADER = "DATE,SYMBOL,SYMBOL_VALUE,USDT" + Utils.NEW_LINE;
    private final Date date;
    private final String symbol;
    private final String symbolValue;
    private final String usdt;
    
    public CsvWalletRow(Date date, String symbol, String symbolValue, String usdt) {
        this.date = date;
        this.symbol = symbol;
        this.symbolValue = symbolValue;
        this.usdt = usdt;
    }

    public Date getDate() {
        return date;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSymbolValue() {
        return symbolValue;
    }

    public String getUsdt() {
        return usdt;
    }
    
    public String toCsvLine() {
        StringBuilder data = new StringBuilder();
        data.append(Utils.fromDate(Utils.FORMAT_SECOND, date))
        .append(",").append(symbol)
        .append(",").append(symbolValue)
        .append(",").append(usdt)
        .append(Utils.NEW_LINE);
        return data.toString();
    }
    
    public static CsvWalletRow fromCsvLine(String line) {
        String[] columns = line.split(",");
        return new CsvWalletRow(Utils.fromString(Utils.FORMAT_SECOND, columns[0]), columns[1], columns[2], columns[3]);
    }
}
