package com.jbescos.cloudbot;

import java.util.Base64;
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
import com.jbescos.common.BinanceAPI;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.Broker;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.SecureBinanceAPI;
import com.jbescos.common.Broker.Action;

//Entry: com.jbescos.cloudbot.BotSubscriber
public class BotSubscriber implements BackgroundFunction<PubSubMessage> {

    private static final Logger LOGGER = Logger.getLogger(BotSubscriber.class.getName());

    @Override
    public void accept(PubSubMessage payload, Context context) throws Exception {
        String data = new String(Base64.getDecoder().decode(payload.data));
        LOGGER.info(() -> "Received: " + data);
        Client client = ClientBuilder.newClient();
        BinanceAPI binanceAPI = new BinanceAPI(client);
        BucketStorage storage = new BucketStorage(StorageOptions.newBuilder().setProjectId(CloudProperties.PROJECT_ID).build().getService(), binanceAPI);
        SecureBinanceAPI api = SecureBinanceAPI.create(client, storage);
        List<Broker> stats = BotUtils.loadStatistics(client, false).stream()
                .filter(stat -> stat.getAction() != Action.NOTHING).collect(Collectors.toList());
        BotBinance bot = new BotBinance(api);
        bot.execute(stats);
        client.close();
    }

    public static class PubSubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
}
