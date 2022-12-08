package com.jbescos.localbot;

import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.Utils;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final long MINUTES_30 = 30 * 60 * 1000;

    public static void main(String[] args) throws IOException, InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ExecutorService executor = Executors.newCachedThreadPool();
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        FileStorage storage = new FileStorage("./crypto-for-training");
        StorageProcess storageProcess = new StorageProcess(publicAPI, storage);
        BotExecutor botProcess = new BotExecutor(client, storage, executor);
        
        scheduler.scheduleAtFixedRate(() -> {
            storageProcess.run();
            botProcess.run();
        }, Utils.delayto30or00(new Date()), MINUTES_30, TimeUnit.MILLISECONDS);
        
        // Wait forever
        scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

}
