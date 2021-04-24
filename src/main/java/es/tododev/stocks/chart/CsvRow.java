package es.tododev.stocks.chart;

import java.util.Date;

public class CsvRow {

	private final Date date;
	private final double value;

	public CsvRow(Date date, double value) {
		this.date = date;
		this.value = value;
	}
	public Date getDate() {
		return date;
	}
	public double getValue() {
		return value;
	}

}
