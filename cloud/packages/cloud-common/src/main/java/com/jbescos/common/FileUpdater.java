package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface FileUpdater {

	String updateFile(String fileName, byte[] content, byte[] header) throws FileNotFoundException, IOException;
}
