package com.jbescos.cloudbot;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.jbescos.cloudbot.BotSubscriber.PubSubMessage;
import com.jbescos.common.BotProcess;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.StorageInfo;
import java.util.Base64;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

//Entry: com.jbescos.cloudbot.BotSubscriber
public class BotSubscriber implements BackgroundFunction<PubSubMessage> {

    @Override
    public void accept(PubSubMessage payload, Context context) throws Exception {
        String userId = new String(Base64.getDecoder().decode(payload.data));
        StorageInfo storageInfo = StorageInfo.build();
        Client client = ClientBuilder.newClient();
        CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
        BucketStorage bucketStorage = new BucketStorage(storageInfo);
        BotProcess process = new BotProcess(cloudProperties, client, bucketStorage);
        process.execute();
        client.close();
    }

    public static class PubSubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
}
