package com.jbescos.common;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.News;
import com.jbescos.exchange.Utils;

public class NewsUtils {

	private static final Logger LOGGER = Logger.getLogger(NewsUtils.class.getName());
	
    public static void news(long millis, PublicAPI publicAPI, CloudProperties cloudProperties, Client client) {
    	try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
	        long minutes30Back = millis - (30 * 60 * 1000);
	        Date rounded = Utils.dateRoundedTo10Min(new Date(minutes30Back));
	        for (Exchange exchange : Exchange.values()) {
	        	try {
		        	List<News> news = exchange.news(publicAPI, rounded.getTime());
		        	for (News n : news) {
		        		telegram.exception(n.toString(), null);
		        	}
	        	} catch (Exception e) {
	        		LOGGER.log(Level.SEVERE, "Cannot obtain news from " + exchange.name(), e);
	        	}
	        }
        }
    }
    
}
