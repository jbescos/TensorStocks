package com.jbescos.localbot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jbescos.localbot.WebSocket.Message;

public class CsvWorker implements MessageWorker<Message> {

	private static final Logger LOGGER = Logger.getLogger(CsvWorker.class.getName());
	private final AtomicBoolean notWorking = new AtomicBoolean(true);
	private final StringBuilder builder = new StringBuilder("DATE,SYMBOL,PRICE,DIRECTION,AVG\r\n");
	private final int FLUSH_MESSAGES = 50;
	private final File CSV;
	private long messages;
	private BigDecimal buyingAvgPrice;
	private BigDecimal sellingAvgPrice;
	
	public CsvWorker(String symbol) {
		CSV = new File(symbol + ".csv");
		LOGGER.info(() -> "CsvWorker instanced for " + symbol);
	}
	
	@Override
	public boolean startToWork() {
		return notWorking.compareAndSet(true, false);
	}
	
	@Override
	public void process(Message message, long now) {
		try {
			messages++;
			String date = Constants.format(new Date(now));
			BigDecimal buyingPrice = new BigDecimal(message.a);
			BigDecimal sellingPrice = new BigDecimal(message.b);
			buyingAvgPrice = Constants.ewma(buyingPrice, buyingAvgPrice);
			sellingAvgPrice = Constants.ewma(sellingPrice, sellingAvgPrice);
			builder.append(date).append(",").append(message.s).append(",").append(buyingPrice).append(",").append("BUY").append(",").append(buyingAvgPrice).append("\r\n");
			builder.append(date).append(",").append(message.s).append(",").append(sellingPrice).append(",").append("SELL").append(",").append(sellingAvgPrice).append("\r\n");
			if (messages > FLUSH_MESSAGES) {
				messages = 0;
				try (FileWriter fw = new FileWriter(CSV, true); BufferedWriter bw = new BufferedWriter(fw);) {
					bw.write(builder.toString());
					bw.flush();
					builder.delete(0, builder.length());
					LOGGER.info(() -> CSV.getAbsolutePath() + " updated");
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Cannot write in " + CSV.getAbsolutePath(), e);
				}
			}
		} finally {
			notWorking.set(true);
		}
	}

}
