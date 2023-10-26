package com.jbescos.common;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Utils;

public class FileActionExecutor {

    private static final Logger LOGGER = Logger.getLogger(FileActionExecutor.class.getName());
    private final CloudProperties cloudProperties;
    private final FileManager storage;
    private final Client client;

    public FileActionExecutor(CloudProperties cloudProperties, FileManager storage, Client client) {
        this.cloudProperties = cloudProperties;
        this.storage = storage;
        this.client = client;
    }

    public void run(String folder, String fileName) throws IOException {
        FileAction action = null;
        if ("profit".equals(folder)) {
            action = new FileActionProfit(cloudProperties, client, storage, fileName);
        } else if ("wallet".equals(folder)) {
            if (isReportTime(new Date())) {
                action = new FileActionWallet(cloudProperties, client, storage);
            }
        }
        if (action != null) {
            LOGGER.info("Running " + action.getClass().getSimpleName());
            action.run();
        }
    }

    // Reports if the bot run between 6:00 and 6:10
    private boolean isReportTime(Date now) {
        return Utils.isTime(now, Utils.REPORT_HOUR);
    }
}
