package com.jbescos.common;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// CREATE TABLE PRICE_HISTORY (SYMBOL varchar(255), PRICE varchar(255), DATE datetime(3));
// CREATE INDEX IDX_DATE ON PRICE_HISTORY (DATE); 
public class DataBase {

	private static final String USER;
	private static final String PASSWORD;
	private static final String URL;
	private static final String DRIVER;

	static {
		try {
			Properties properties = Utils.fromClasspath("/database.properties");
			USER = properties.getProperty("database.user");
			PASSWORD = properties.getProperty("database.password");
			URL = properties.getProperty("database.url");
			DRIVER = properties.getProperty("database.driver");
			Class.forName(DRIVER);
		} catch (ClassNotFoundException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public List<Map<String, Object>> get(String query, Object... args) throws SQLException {
		List<Map<String, Object>> results = new ArrayList<>();
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement ps = connection.prepareStatement(query);) {
			for (int i = 0; i < args.length; i++) {
				ps.setObject(i + 1, args[i]);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					int max = rs.getMetaData().getColumnCount();
					for (int i = 0; i < max; i++) {
						row.put(rs.getMetaData().getColumnLabel(i + 1), rs.getObject(i + 1));
					}
					results.add(row);
				}
			}
		}
		return results;
	}

	public int insert(String sqlQuery, List<Price> rows, Date date) throws SQLException {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
				PreparedStatement ps = connection.prepareStatement(sqlQuery);) {
			connection.setAutoCommit(false);
			for (Price price : rows) {
				ps.setString(1, price.getSymbol());
				ps.setDouble(2, price.getPrice());
				ps.setObject(3, date);
				ps.addBatch();
			}
			int result = ps.executeBatch().length;
			connection.commit();
			return result;
		}
	}

}
