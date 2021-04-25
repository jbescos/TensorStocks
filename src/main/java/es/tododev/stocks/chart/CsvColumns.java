package es.tododev.stocks.chart;

import es.tododev.stocks.yahoo.PriceItem;

public enum CsvColumns {

	date(0) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setDate(Long.parseLong(cols[getColumnIdx()]));
		}
	}, adjclose(1) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setAdjclose(Double.parseDouble(cols[getColumnIdx()]));
		}
	}, open(2) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setOpen(Double.parseDouble(cols[getColumnIdx()]));
		}
	}, high(3) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setHigh(Double.parseDouble(cols[getColumnIdx()]));
		}
	}, low(4) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setLow(Double.parseDouble(cols[getColumnIdx()]));
		}
	}, close(5) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setClose(Double.parseDouble(cols[getColumnIdx()]));
		}
	}, volume(6) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setVolume(Long.parseLong(cols[getColumnIdx()]));
		}
	}, symbol(7) {
		@Override
		public void addValue(PriceItem item, String[] cols) {
			item.setSymbol(Integer.parseInt(cols[getColumnIdx()]));
		}
	};

	private final int columnIdx;

	private CsvColumns(int columnIdx) {
		this.columnIdx = columnIdx;
	}

	public int getColumnIdx() {
		return columnIdx;
	}

	public abstract void addValue(PriceItem item, String[] cols);
}
