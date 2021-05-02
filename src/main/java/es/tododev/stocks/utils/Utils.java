package es.tododev.stocks.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class Utils {
	
	private static final Map<Integer, String> SYMBOLS_BY_ID = new HashMap<>();
	private static final Map<String, Integer> IDS_BY_SYMBOL = new HashMap<>();
	private static final Map<String, String> SYMBOLS = new HashMap<>();
	private static final String SYMBOLS_PROPERTIES = "/symbols.properties";

	static {
		try {
			Properties prop = fromClasspath(SYMBOLS_PROPERTIES);
			for (Entry<Object, Object> entry : prop.entrySet()) {
				String[] value = entry.getValue().toString().split(",");
				String key = entry.getKey().toString();
				SYMBOLS.put(key, value[0]);
				SYMBOLS_BY_ID.put(Integer.parseInt(value[1]), value[0]);
				IDS_BY_SYMBOL.put(value[0], Integer.parseInt(value[1]));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load required " + SYMBOLS_PROPERTIES, e);
		}
	}

	public static Date fromTimestamp(long timestamp) {
		Date date = new Date(timestamp * 1000);
		return date;
	}
	
	public static void writeInFile(File file, String content) throws IOException {
		try(FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] bytes = content.getBytes();
            bos.write(bytes);
        }
	}
	
	public static Properties fromClasspath(String properties) throws IOException {
		Properties prop = new Properties();
		try (InputStream in = Utils.class.getResourceAsStream(properties)) {
			prop.load(in);
		}
		return prop;
	}

	public static Properties fromPath(String path) throws IOException {
		File file = new File(path);
		if (!file.exists()) {
			throw new IllegalArgumentException(path + " was not found");
		}
		Properties prop = new Properties();
		try (FileInputStream stream = new FileInputStream(path)) { 
			prop.load(new FileInputStream(path));
		}
		return prop;
	}

	public static int getSymbol(String symbol) {
		return IDS_BY_SYMBOL.get(symbol);
	}
	
	public static String getSymbol(int id) {
		return SYMBOLS_BY_ID.get(id);
	}
	
	public static Map<String, String> getSymbols() {
		return Collections.unmodifiableMap(SYMBOLS);
	}
}
