package com.jbescos.test.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.jbescos.common.FileUpdater;

public class TestFileStorage implements FileUpdater {

	private final String filePath;
	
	public TestFileStorage(String filePath) {
		this.filePath = filePath;
	}
	
	@Override
	public String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException {
		Path path = Paths.get(filePath + fileName);
		File file = path.toFile();
		if (!file.exists()) {
			Files.write(path, header, StandardOpenOption.CREATE);
		}
		Files.write(path, content, StandardOpenOption.APPEND);
		return file.getAbsolutePath();
	}

}
