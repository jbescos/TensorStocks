package com.jbescos.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.jbescos.common.CsvUtil;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.Utils;

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
            List<CsvTransactionRow> newTx = CsvUtil.readCsvTransactionRows(false, ",",
                    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content))));
            transactions.addAll(newTx);
        } else if ("profit.csv".equals(fileName)) {
            List<CsvProfitRow> newProfits = CsvUtil.readCsvProfitRows(false,
                    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content))));
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
            List<CsvTransactionRow> newTx = CsvUtil.readCsvTransactionRows(false, ",",
                    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content))));
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
    public List<CsvRow> loadPreviousRows(String exchangeFolder, int hoursBack, List<String> whiteListSymbols)
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
        storage.updateFile("transactions.csv", data.toString().getBytes(Utils.UTF8),
                Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
        data.setLength(0);
        openTransactions.stream().forEach(r -> data.append(r.toCsvLine()));
        storage.updateFile("open_possitions.csv", data.toString().getBytes(Utils.UTF8),
                Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
        data.setLength(0);
        profit.stream().forEach(r -> data.append(r.toCsvLine()));
        storage.updateFile("profit.csv", data.toString().getBytes(Utils.UTF8),
                CsvProfitRow.HEADER.getBytes(Utils.UTF8));
    }

    public void setBaseUsdt(String baseUsdt) {
        this.baseUsdt = baseUsdt;
    }

    @Override
    public String getRaw(String file) {
        if (file.endsWith(Utils.CONTEXT_DATA_FILE)) {
            return baseUsdt;
        } else if (file.endsWith(Utils.NEWS_SUBFIX)) {
            return null;
        } else {
            throw new IllegalArgumentException("No definition for " + file + " in memory");
        }
    }

    @Override
    public Map<String, CsvRow> previousRows(String lastUpdated) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<CsvRow> updatedRowsAndSaveLastPrices(Map<String, CsvRow> previousRows, Map<String, Price> prices,
            Date now, String lastPriceCsv, int fearGreedIndex) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<CsvProfitRow> loadCsvProfitRows(String profitFile) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<CsvTransactionRow> loadCsvTransactionRows(String txFile) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Map<String, String> loadWallet(String walletFile) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public <T> List<T> loadRows(String file, Function<BufferedReader, List<T>> function) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
