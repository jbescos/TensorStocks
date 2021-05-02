package es.tododev.stocks.binance;

import javax.ws.rs.core.GenericType;

public interface OperatorAPI {

	<T> T request(String path, String query, boolean signed, boolean post, GenericType<T> type);

}
