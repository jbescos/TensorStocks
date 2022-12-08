package com.jbescos.localbot;

import com.jbescos.common.BotProcess;
import com.jbescos.common.CloudProperties;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;

public class BotExecutor {

    private static final Logger LOGGER = Logger.getLogger(BotExecutor.class.getName());
    private final Client client;
    private final FileStorage storage;
    private final ExecutorService executor;

    public BotExecutor(Client client, FileStorage storage, ExecutorService executor) {
        this.client = client;
        this.storage = storage;
        this.executor = executor;
    }
    
    public void run() {
        try {
            List<CloudProperties> properties = storage.loadProperties("./crypto-properties");
            for (CloudProperties property : properties) {
                executor.submit(() -> {
                    BotProcess process = new BotProcess(property, client, storage);
                    try {
                        process.execute();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error executing bot for " + property.USER_ID, e);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot load user properties", e);
        }
        
    }
}
