package com.jbescos.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Utils {
	
	public static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat FORMAT_SECOND = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final Charset UTF = Charset.forName("UTF-8");

	public static Properties fromClasspath(String properties) throws IOException {
		Properties prop = new Properties();
		try (InputStream in = DataBase.class.getResourceAsStream(properties)) {
			prop.load(in);
		}
		return prop;
	}
	
	public static Date fromString(DateFormat format, String date) {
		try {
			return format.parse(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Cannot parse " + date, e);
		}
	}
}