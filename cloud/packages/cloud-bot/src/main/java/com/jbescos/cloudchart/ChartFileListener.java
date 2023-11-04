package com.jbescos.cloudchart;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.jbescos.cloudchart.ChartFileListener.GCSEvent;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.FileActionExecutor;
import com.jbescos.common.StorageInfo;
import com.jbescos.exchange.FileManager;

// Entry: com.jbescos.cloudchart.ChartFileListener
public class ChartFileListener implements BackgroundFunction<GCSEvent> {

    private static final Logger LOGGER = Logger.getLogger(ChartFileListener.class.getName());

    // Finalize or create
    @Override
    public void accept(GCSEvent event, Context context) {
        LOGGER.info("Processing file: " + event.name);
        // test-Kucoin/profit/profit_2023-03.csv
        // test-Kucoin/wallet/wallet_2023-03.csv
        String[] folders = event.name.split("/");
        String folder = folders[folders.length - 2];
        String userId = folders[folders.length - 3];
        try {
            // Exclude the folder of data because is not an user
            if (!"data".equals(userId)) {
                if ("profit".equals(folder) || "wallet".equals(folder) || "tx_summary".equals(folder) ) {
                    StorageInfo storageInfo = StorageInfo.build();
                    FileManager storage = new BucketStorage(storageInfo);
                    CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
                    Client client = ClientBuilder.newClient();
                    new FileActionExecutor(cloudProperties, storage, client).run(folder, event.name);
                    client.close();
                }
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot process " + event.name, e);
        }
    }
    
    public static class GCSEvent {
        String bucket;
        String name;
        String metageneration;
    }
}
