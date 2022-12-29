package com.jbescos.localbot;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.FileManager;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.Utils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

public class FileStorage implements FileManager {

    private final String filePath;

    public FileStorage(String filePath) {
        this.filePath = filePath;
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
    public String overwriteFile(String fileName, byte[] content, byte[] header)
            throws FileNotFoundException, IOException {
        Path path = Paths.get(filePath + fileName);
        File file = path.toFile();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (header != null) {
            outputStream.write(header);
        }
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
    public List<CsvTransactionRow> loadOpenTransactions(String userId) throws IOException {
        byte[] content = get(userId + "/" + Utils.OPEN_POSSITIONS);
        List<CsvTransactionRow> transactions = Collections.emptyList();
        if (content != null) {
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content, Utils.UTF8)))) {
                transactions = CsvUtil.readCsvTransactionRows(true, ",", reader);
            }
        }
        return transactions;
    }

    @Override
    public List<CsvRow> loadPreviousRows(Exchange exchange, int hoursBack, List<String> whiteListSymbols)
            throws IOException {
        // Get 1 day more and compare dates later
        List<String> days = Utils.daysBack(new Date(), (hoursBack / 24) + 1, "data" + exchange.getFolder(), ".csv");
        List<CsvRow> rows = new ArrayList<>();
        Date now = new Date();
        Date from = Utils.getDateOfHoursBack(now, hoursBack);
        List<CsvRow> csvInDay = null;
        for (String day : days) {
            byte[] content = get(day);
            if (content == null) {
                throw new IllegalStateException(day + " was not found");
            }
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content, Utils.UTF8)))) {
                csvInDay = CsvUtil.readCsvRows(true, ",", reader, whiteListSymbols);
                csvInDay = csvInDay.stream().filter(row -> row.getDate().getTime() > from.getTime())
                        .collect(Collectors.toList());
                rows.addAll(csvInDay);
            }
        }
        return rows;
    }

    @Override
    public List<CsvProfitRow> loadCsvProfitRows(String userId, int monthsBack) {
        List<String> months = Utils.monthsBack(new Date(), monthsBack, userId + "/" + CsvProfitRow.PREFIX, ".csv");
        List<CsvProfitRow> rows = new ArrayList<>();
        for (String month : months) {
            rows.addAll(loadCsvProfitRows(month));
        }
        return rows;
    }
    
    @Override
    public List<CsvProfitRow> loadCsvProfitRows(String profitFile) {
        List<CsvProfitRow> profitRows = Collections.emptyList();
        byte[] content = get(profitFile);
        if (content != null) {
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content, Utils.UTF8)))) {
                profitRows = CsvUtil.readCsvProfitRows(reader);
            } catch (Exception e) {
                // Eat it, no CSV was found is not an error
            }
        }
        return profitRows;
    }

    @Override
    public List<CsvTransactionRow> loadCsvTransactionRows(String txFile) {
        List<CsvTransactionRow> transactions = Collections.emptyList();
        byte[] content = get(txFile);
        if (content != null) {
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content, Utils.UTF8)))) {
                transactions = CsvUtil.readCsvTransactionRows(true, ",", reader);
            } catch (Exception e) {
                // Eat it, no CSV was found is not an error
            }
        }
        return transactions;
    }

    @Override
    public String getRaw(String file) {
        byte[] data = get(file);
        if (data != null) {
            return new String(data, Utils.UTF8);
        }
        return null;
    }

    @Override
    public Map<String, CsvRow> previousRows(String lastUpdated) throws IOException {
        Map<String, CsvRow> previousRows = new LinkedHashMap<>();
        byte[] content = get(lastUpdated);
        if (content != null) {
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content, Utils.UTF8)))) {
                List<CsvRow> csv = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
                for (CsvRow row : csv) {
                    previousRows.put(row.getSymbol(), row);
                }
            }
        }
        return previousRows;
    }
    
    private byte[] get(String path) {
        File file = new File(filePath + path);
        if (file.exists()) {
            try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException e) {}
        }
        return null;
    }

    @Override
    public List<CsvRow> updatedRowsAndSaveLastPrices(Map<String, CsvRow> previousRows, Map<String, Price> prices,
            Date now, String lastPriceCsv, int fearGreedIndex) throws IOException {
        StringBuilder builder = new StringBuilder(Utils.CSV_ROW_HEADER);
        List<CsvRow> newRows = new ArrayList<>();
        for (Entry<String, Price> price : prices.entrySet()) {
            CsvRow previous = previousRows.get(price.getKey());
            CsvRow newRow = null;
            if (previous != null) {
                newRow = new CsvRow(now, price.getValue(),
                        Utils.ewma(Utils.EWMA_CONSTANT, price.getValue().getPrice(), previous.getAvg()),
                        Utils.ewma(Utils.EWMA_2_CONSTANT, price.getValue().getPrice(), previous.getAvg2()),
                        fearGreedIndex, Utils.dynamicEwma(Utils.EWMA_CONSTANT, Utils.EWMA_2_CONSTANT, fearGreedIndex,
                                previous.getFearGreedIndexAvg()));
            } else {
                newRow = new CsvRow(now, price.getValue(), price.getValue().getPrice(), price.getValue().getPrice(),
                        fearGreedIndex, (double) fearGreedIndex);
            }
            newRows.add(newRow);
            builder.append(newRow.toCsvLine());
        }
        overwriteFile(lastPriceCsv, builder.toString().getBytes(Utils.UTF8), null);
        return newRows;
    }

    public List<CloudProperties> loadProperties(String path) throws IOException {
        List<CloudProperties> properties = new ArrayList<>();
        File mainProperties = new File(path + CloudProperties.PROPERTIES_FILE);
        if (!mainProperties.exists()) {
            throw new IllegalStateException(mainProperties.getAbsolutePath() + " is required");
        }
        Properties mainProp = load(mainProperties);
        File directoryPath = new File(path);
        for (File file : directoryPath.listFiles()) {
            if (file.isDirectory()) {
                String userId = file.getName();
                File child = file.listFiles()[0];
                Properties idProp = load(child);
                CloudProperties cloudProperties = new CloudProperties(userId, mainProp, idProp);
                if (cloudProperties.USER_ACTIVE) {
                    properties.add(cloudProperties);
                }
            }
        }
        return properties;
    }
    
    public CloudProperties loadMainProperties(String path) throws IOException {
    	File mainProperties = new File(path + CloudProperties.PROPERTIES_FILE);
        if (!mainProperties.exists()) {
            throw new IllegalStateException(mainProperties.getAbsolutePath() + " is required");
        }
        Properties mainProp = load(mainProperties);
        CloudProperties properties = new CloudProperties(null, mainProp, new Properties());
        return properties;
    }
    
    private Properties load(File file) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }
}
