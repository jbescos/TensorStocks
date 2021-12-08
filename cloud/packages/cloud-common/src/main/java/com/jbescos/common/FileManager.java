package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface FileManager {

	String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException;
	
	List<CsvTransactionRow> loadTransactions() throws IOException;
	
	List<CsvRow> loadPreviousRows() throws IOException;
}
