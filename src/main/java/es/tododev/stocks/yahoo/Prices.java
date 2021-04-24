package es.tododev.stocks.yahoo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Prices {

	private List<PriceItem> prices = new ArrayList<>();
	private boolean isPending;
	private long firstTradeDate; //Timestamp
	private String id;
	private Map<String, Integer> timeZone = new HashMap<>();
	private Object[] eventsData = new Object[0];

	public void setPrices(List<PriceItem> prices) {
		this.prices = prices;
	}

	public List<PriceItem> getPrices() {
		return prices;
	}

	public boolean isIsPending() {
		return isPending;
	}

	public void setIsPending(boolean isPending) {
		this.isPending = isPending;
	}

	public long getFirstTradeDate() {
		return firstTradeDate;
	}

	public void setFirstTradeDate(long firstTradeDate) {
		this.firstTradeDate = firstTradeDate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, Integer> getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(Map<String, Integer> timeZone) {
		this.timeZone = timeZone;
	}

	public Object[] getEventsData() {
		return eventsData;
	}

	public void setEventsData(Object[] eventsData) {
		this.eventsData = eventsData;
	}

	@Override
	public String toString() {
		return "Prices [prices=" + prices + ", isPending=" + isPending + ", firstTradeDate=" + firstTradeDate + ", id="
				+ id + ", timeZone=" + timeZone + ", eventsData=" + Arrays.toString(eventsData) + "]";
	}

}
