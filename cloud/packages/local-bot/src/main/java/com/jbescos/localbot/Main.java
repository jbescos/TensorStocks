package com.jbescos.localbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.exchange.PublicAPI;

public class Main {

    private static final Logger LOGGER;
    
    static {
        try (InputStream configFile = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(configFile);
            LOGGER = Logger.getLogger(Main.class.getName());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize the logger", e);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        LOGGER.info("Starting local-bot");
        ExecutorService executor = Executors.newCachedThreadPool();
        Client client = ClientBuilder.newClient();
        PublicAPI publicAPI = new PublicAPI(client);
        FileStorage storage = new FileStorage("./crypto-for-training/");
        StorageProcess storageProcess = new StorageProcess(publicAPI, storage);
        BotExecutor botProcess = new BotExecutor(client, storage, executor);
        storageProcess.run();
        botProcess.run();
        executor.shutdown();
        LOGGER.info("Awaiting tasks to complete");
        executor.awaitTermination(20, TimeUnit.MINUTES);
        LOGGER.info("Closing local-bot");
        client.close();
    }

}
