package es.tododev.stocks.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class Utils {

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

}
