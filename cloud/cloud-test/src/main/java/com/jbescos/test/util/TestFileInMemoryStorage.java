package com.jbescos.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.Utils;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.FileManager;

public class TestFileInMemoryStorage implements FileManager {

    private final List<CsvTransactionRow> transactions;
    private final List<CsvTransactionRow> openTransactions;
    private final List<CsvRow> previousRows;
    private final List<CsvProfitRow> profit;
    private String baseUsdt;
    
    public TestFileInMemoryStorage(List<CsvTransactionRow> transactions, List<CsvTransactionRow> openTransactions,
            List<CsvRow> previousRows, List<CsvProfitRow> profit) {
        this.transactions = transactions;
        this.openTransactions = openTransactions;
        this.previousRows = previousRows;
        this.profit = profit;
    }

    @Override
    public String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException {
        if ("transactions.csv".equals(fileName)) {
            List<CsvTransactionRow> newTx = CsvUtil.readCsvTransactionRows(false, ",", new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content))));
            transactions.addAll(newTx);
        } else if ("profit.csv".equals(fileName)) {
            List<CsvProfitRow> newProfits = CsvUtil.readCsvProfitRows(false, new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content))));
            profit.addAll(newProfits);
        } else {
            throw new IllegalArgumentException(fileName + " is unkown for test in memory");
        }
        return null;
    }

    @Override
    public String overwriteFile(String fileName, byte[] content, byte[] header)
            throws FileNotFoundException, IOException {
        if ("open_possitions.csv".equals(fileName)) {
            openTransactions.clear();
            List<CsvTransactionRow> newTx = CsvUtil.readCsvTransactionRows(false, ",", new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content))));
            openTransactions.addAll(newTx);
        } else {
            throw new IllegalArgumentException(fileName + " is unkown for test in memory");
        }
        return null;
    }

    @Override
    public List<CsvTransactionRow> loadOpenTransactions(String userId) throws IOException {
        return openTransactions;
    }

    @Override
    public List<CsvRow> loadPreviousRows(Exchange exchange, int hoursBack, List<String> whiteListSymbols)
            throws IOException {
        return previousRows;
    }

    @Override
    public List<CsvProfitRow> loadCsvProfitRows(String userId, int monthsBack) {
        return profit;
    }

    public void persist(String path) throws IOException {
        TestFileStorage storage = new TestFileStorage(path, transactions, previousRows);
        StringBuilder data = new StringBuilder();
        transactions.stream().forEach(r -> data.append(r.toCsvLine()));
        storage.updateFile("transactions.csv", data.toString().getBytes(Utils.UTF8), Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
        data.setLength(0);
        openTransactions.stream().forEach(r -> data.append(r.toCsvLine()));
        storage.updateFile("open_possitions.csv", data.toString().getBytes(Utils.UTF8), Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
        data.setLength(0);
        profit.stream().forEach(r -> data.append(r.toCsvLine()));
        storage.updateFile("profit.csv", data.toString().getBytes(Utils.UTF8), CsvProfitRow.HEADER.getBytes(Utils.UTF8));
    }

    public void setBaseUsdt(String baseUsdt) {
        this.baseUsdt = baseUsdt;
    }

    @Override
    public String getRaw(String file) {
        return baseUsdt;
    }
}
