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
import com.google.cloud.storage.StorageOptions;
import com.jbescos.cloudbot.BotSubscriber.PubSubMessage;
import com.jbescos.common.Account;
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.Broker;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Price;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Utils;
import com.jbescos.common.Broker.Action;

//Entry: com.jbescos.cloudbot.BotSubscriber
public class BotSubscriber implements BackgroundFunction<PubSubMessage> {

    private static final Logger LOGGER = Logger.getLogger(BotSubscriber.class.getName());
    private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);

    @Override
    public void accept(PubSubMessage payload, Context context) throws Exception {
        String data = new String(Base64.getDecoder().decode(payload.data));
        LOGGER.info(() -> "Received: " + data);
        Client client = ClientBuilder.newClient();
        BinanceAPI binanceAPI = new BinanceAPI(client);
        long time = binanceAPI.time();
        Date now = new Date(time);
        BucketStorage storage = new BucketStorage(StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService(), binanceAPI);
        SecureBinanceAPI api = SecureBinanceAPI.create(client, storage);
        List<Broker> stats = BotUtils.loadStatistics(client, false).stream()
                .filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
        BotBinance bot = new BotBinance(api);
        bot.execute(stats);
        // Update wallet
        Account account = api.account();
        List<Price> prices = binanceAPI.price();
        List<Map<String, String>> rows = Utils.userUsdt(now, prices, account);
        storage.updateFile(Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv", CsvUtil.toString(rows).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
        client.close();
    }

    public static class PubSubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
}
