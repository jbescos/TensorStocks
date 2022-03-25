package com.jbescos.cloudbot;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.jbescos.cloudbot.BotSubscriber.PubSubMessage;
import com.jbescos.common.Broker;
import com.jbescos.common.BrokerManager;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvTxSummaryRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.DefaultBrokerManager;
import com.jbescos.common.PublicAPI;
import com.jbescos.common.SecuredAPI;
import com.jbescos.common.StorageInfo;
import com.jbescos.common.TelegramBot;
import com.jbescos.common.TransactionsSummary;
import com.jbescos.common.Utils;

//Entry: com.jbescos.cloudbot.BotSubscriber
public class BotSubscriber implements BackgroundFunction<PubSubMessage> {

    private static final Logger LOGGER = Logger.getLogger(BotSubscriber.class.getName());
    private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);

    @Override
    public void accept(PubSubMessage payload, Context context) throws Exception {
    	long millis = System.currentTimeMillis();
        String userId = new String(Base64.getDecoder().decode(payload.data));
        StorageInfo storageInfo = StorageInfo.build();
        Client client = ClientBuilder.newClient();
        CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
        PublicAPI publicAPI = new PublicAPI(client);
        long time = publicAPI.time();
        Date now = new Date(time);
        BucketStorage bucketStorage = new BucketStorage(storageInfo);
        BrokerManager brokerManager = new DefaultBrokerManager(cloudProperties, bucketStorage);
        SecuredAPI securedApi = cloudProperties.USER_EXCHANGE.create(cloudProperties, client);
        List<Broker> brokers = brokerManager.loadBrokers();
        BotExecution bot = BotExecution.production(cloudProperties, securedApi, bucketStorage);
        bot.execute(brokers);

        // Update wallet in case the exchange supports it
        if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
	        Map<String, String> wallet = securedApi.wallet();
	        Map<String, Double> prices = cloudProperties.USER_EXCHANGE.price(publicAPI);
	        List<Map<String, String>> rows = Utils.userUsdt(now, prices, wallet);
	        bucketStorage.updateFile(cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv", CsvUtil.toString(rows).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
        }
        Map<String, Double> benefits = new HashMap<>();
        for (Broker broker : brokers) {
        	TransactionsSummary summary = broker.getPreviousTransactions();
        	if (summary.isHasTransactions()) {
    		    benefits.put(broker.getSymbol(), Utils.benefit(summary.getMinProfitable(), broker.getNewest().getPrice()));
    		}
        }
        LOGGER.info(() -> cloudProperties.USER_ID + ": Summary of benefits " + benefits);
        String body = CsvTxSummaryRow.toCsvBody(now, benefits);
        bucketStorage.updateFile(cloudProperties.USER_ID + "/" + Utils.TX_SUMMARY_PREFIX + Utils.fromDate(Utils.FORMAT, now) + ".csv", body.getBytes(Utils.UTF8), CsvTxSummaryRow.CSV_HEADER_TX_SUMMARY_TOTAL);
        
        // Report
        boolean report = isReportTime(now, cloudProperties);
        if (report) {
            TelegramBot telegram = new TelegramBot(cloudProperties, client);
            if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
            	telegram.sendHtmlLink();
            }
        }
        client.close();
        LOGGER.info(userId + ": function took " + ((System.currentTimeMillis() - millis) / 1000) + " seconds");
    }
    
    // Reports if the bot run between 6:00 and 6:10
    private boolean isReportTime(Date now, CloudProperties cloudProperties) {
        if (cloudProperties.TELEGRAM_BOT_ENABLED) {
            return Utils.isTime(now, 6);
        }
        return false;
    }

    public static class PubSubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
}
