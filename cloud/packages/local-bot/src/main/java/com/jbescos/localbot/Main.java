package com.jbescos.localbot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.FileActionExecutor;
import com.jbescos.common.FileStorage;
import com.jbescos.common.FileStorage.FileListener;

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
        LOGGER.info("Starting local-bot " + Arrays.asList(args));
        List<Exchange> exchanges;
        if (args.length != 0) {
            String[] exchangesArr = args[0].split(",");
            exchanges = Arrays.asList(exchangesArr).stream().map(ex -> Exchange.valueOf(ex)).collect(Collectors.toList());
        } else {
            exchanges = Arrays.asList(Exchange.values());
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        Client client = ClientBuilder.newClient();
        LocalFileListener listener = new LocalFileListener();
        FileStorage storage = new FileStorage("./crypto-for-training/", listener);
        LocalProcess storageProcess = new LocalProcess(executor, client, storage);
        List<CloudProperties> properties = storageProcess.run(exchanges);
        executor.shutdown();
        LOGGER.info("Awaiting tasks to complete");
        executor.awaitTermination(20, TimeUnit.MINUTES);
        printCharts(storage, listener, client, properties);
        client.close();
        LOGGER.info("Closing local-bot");
    }

    private static void printCharts(FileStorage storage, LocalFileListener listener, Client client, List<CloudProperties> properties) {
        listener.modifiedFiles.parallelStream().forEach(file -> {
            String[] folders = file.split("/");
            String folder = folders[folders.length - 2];
            String userId = folders[folders.length - 3];
            LOGGER.info("Processing " + file + ", folder = " + folder + ", userId = " + userId);
            // Exclude the folder of data because is not an user
            if (!"data".equals(userId)) {
                for (CloudProperties property : properties) {
                    if (property.USER_ID.equals(userId)) {
                        try {
                            new FileActionExecutor(property, storage, client).run(folder, file);
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Cannot print charts of " + file, e);
                        }
                        break;
                    }
                }
            }
        });
    }
    
    private static class LocalFileListener implements FileListener {

        private final Set<String> modifiedFiles = Collections.synchronizedSet(new HashSet<>());
        
        @Override
        public void onModify(String file) {
            modifiedFiles.add(file);
        }
        
    }
}
