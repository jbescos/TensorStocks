package com.jbescos.test.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
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

public class TestFileStorage implements FileManager {

    private final String filePath;
    private final List<CsvTransactionRow> transactions;
    private final List<CsvRow> previousRows;

    public TestFileStorage(String filePath, List<CsvTransactionRow> transactions, List<CsvRow> previousRows) {
        this.filePath = filePath;
        this.transactions = transactions;
        this.previousRows = previousRows;
    }

    @Override
    public String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException {
        Path path = Paths.get(filePath + fileName);
        File file = path.toFile();
        if (!file.exists()) {
            Files.createDirectories(path.getParent());
            if (header != null) {
                Files.write(path, header, StandardOpenOption.CREATE);
            }
        }
        Files.write(path, content, StandardOpenOption.APPEND);
        return file.getAbsolutePath();
    }

    @Override
    public List<CsvTransactionRow> loadOpenTransactions(String userId) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<CsvRow> loadPreviousRows(String exchangeFolder, int hoursBack, List<String> whiteListSymbols)
            throws IOException {
        return previousRows;
    }

    @Override
    public String overwriteFile(String fileName, byte[] content, byte[] header)
            throws FileNotFoundException, IOException {
        Path path = Paths.get(filePath + fileName);
        File file = path.toFile();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(header);
        outputStream.write(content);
        if (!file.exists()) {
            Files.createDirectories(path.getParent());
        } else {
            file.delete();
        }
        Files.write(path, outputStream.toByteArray(), StandardOpenOption.CREATE);
        return file.getAbsolutePath();
    }

    @Override
    public List<CsvProfitRow> loadCsvProfitRows(String userId, int monthsBack) {
        Path path = Paths.get(filePath + "/profitable.csv");
        File file = path.toFile();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            return CsvUtil.readCsvProfitRows(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRaw(String file) {
        throw new UnsupportedOperationException("Not implemented");
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
        String data = getRaw(file);
        if (data != null) {
            return function.apply(new BufferedReader(new StringReader(data)));
        } else {
            return Collections.emptyList();
        }
    }

}
