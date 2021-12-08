package com.jbescos.cloudbot;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.jbescos.cloudbot.BotSubscriber.PubSubMessage;
import com.jbescos.common.Broker;
import com.jbescos.common.Broker.Action;
import com.jbescos.common.BrokerManager;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.DefaultBrokerManager;
import com.jbescos.common.PublicAPI;
import com.jbescos.common.SecuredAPI;
import com.jbescos.common.Utils;

//Entry: com.jbescos.cloudbot.BotSubscriber
public class BotSubscriber implements BackgroundFunction<PubSubMessage> {

    private static final Logger LOGGER = Logger.getLogger(BotSubscriber.class.getName());
    private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);

    @Override
    public void accept(PubSubMessage payload, Context context) throws Exception {
        String userId = new String(Base64.getDecoder().decode(payload.data));
        LOGGER.info(() -> "Received: " + userId);
        CloudProperties cloudProperties = new CloudProperties(userId);
        LOGGER.info(() -> "Openning HTTP client in: " + userId);
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        long time = publicAPI.time();
        LOGGER.info(() -> "Server time is: " + time);
        Date now = new Date(time);
        Storage storage = StorageOptions.newBuilder().setProjectId(cloudProperties.PROJECT_ID).build().getService();
        BucketStorage bucketStorage = new BucketStorage(cloudProperties, storage);
        BrokerManager brokerManager = new DefaultBrokerManager(cloudProperties, bucketStorage);
        SecuredAPI securedApi = cloudProperties.USER_EXCHANGE.create(cloudProperties, client);
        List<Broker> stats = brokerManager.loadBrokers().stream()
                .filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
        BotExecution bot = BotExecution.production(cloudProperties, securedApi, bucketStorage);
        bot.execute(stats);
        // Update wallet in case the exchange supports it
        if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
	        Map<String, String> wallet = securedApi.wallet();
	        Map<String, Double> prices = cloudProperties.USER_EXCHANGE.price(publicAPI);
	        List<Map<String, String>> rows = Utils.userUsdt(now, prices, wallet);
	        bucketStorage.updateFile(cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv", CsvUtil.toString(rows).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
        }
        client.close();
    }

    public static class PubSubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
}
