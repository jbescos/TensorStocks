package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.CsvTransactionRow;

public interface FileManager {

	String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException;
	
	String overwriteFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException;
	
	List<CsvTransactionRow> loadOpenTransactions(String userId) throws IOException;
	
	List<CsvRow> loadPreviousRows(Exchange exchange, int hoursBack, List<String> whiteListSymbols) throws IOException;

	List<CsvProfitRow> loadCsvProfitRows(String userId, int monthsBack);
	
	String getRaw(String file);
}
