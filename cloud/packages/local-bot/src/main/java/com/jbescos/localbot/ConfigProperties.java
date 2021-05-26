package com.jbescos.localbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class ConfigProperties {

	private static final String PROPERTY_PATH = System.getProperty("property");
	public static final String WS_URL;
	public static final List<String> SYMBOLS;
	public static final long LATENCY;
	
	static {
		try {
			Properties properties = load(PROPERTY_PATH);
			WS_URL = properties.getProperty("binance.ws.url");
			SYMBOLS = Arrays.asList(properties.getProperty("bot.symbols").toLowerCase().split(","));
			LATENCY = Long.parseLong(properties.getProperty("message.millis.latency"));
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load properties " + PROPERTY_PATH, e);
		}
	}
	
	private static Properties load(String properties) throws IOException {
		if (properties == null) {
			try (InputStream in = ConfigProperties.class.getResourceAsStream("/config.properties")) {
				return load(in);
			}
		} else {
			try (FileInputStream fileInput = new FileInputStream(new File(PROPERTY_PATH))) {
				return load(fileInput);
			}
		}
	}
	
	private static Properties load(InputStream in) throws IOException {
		if (in != null) {
			Properties prop = new Properties();
			prop.load(in);
			return prop;
		}
		return null;
	}
	
}
