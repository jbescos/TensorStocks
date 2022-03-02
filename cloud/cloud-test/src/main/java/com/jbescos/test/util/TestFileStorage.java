package com.jbescos.test.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.FileManager;

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
			Files.write(path, header, StandardOpenOption.CREATE);
		}
		Files.write(path, content, StandardOpenOption.APPEND);
		return file.getAbsolutePath();
	}

	@Override
	public List<CsvTransactionRow> loadTransactions() throws IOException {
		// FIXME read only open positions
		return transactions;
	}

	@Override
	public List<CsvRow> loadPreviousRows() throws IOException {
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

}
