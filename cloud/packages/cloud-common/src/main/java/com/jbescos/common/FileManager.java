package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.Price;

public interface FileManager {

    String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException;

    String overwriteFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException;

    List<CsvTransactionRow> loadOpenTransactions(String userId) throws IOException;

    List<CsvRow> loadPreviousRows(Exchange exchange, int hoursBack, List<String> whiteListSymbols) throws IOException;
    
    Map<String, CsvRow> previousRows(String lastUpdated) throws IOException;

    List<CsvProfitRow> loadCsvProfitRows(String userId, int monthsBack);

    String getRaw(String file);

    List<CsvRow> updatedRowsAndSaveLastPrices(Map<String, CsvRow> previousRows, Map<String, Price> prices, Date now,
            String lastPriceCsv, int fearGreedIndex) throws IOException;

    List<CsvProfitRow> loadCsvProfitRows(String profitFile);

    List<CsvTransactionRow> loadCsvTransactionRows(String txFile);
}
