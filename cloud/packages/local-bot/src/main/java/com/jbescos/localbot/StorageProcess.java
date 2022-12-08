package com.jbescos.localbot;

import com.jbescos.common.CloudProperties.Exchange;
import com.jbescos.common.FileManager;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.FearGreedIndex;
import com.jbescos.exchange.Price;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.Utils;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageProcess {

    private static final Logger LOGGER = Logger.getLogger(StorageProcess.class.getName());
    private final PublicAPI publicAPI;
    private final FileManager storage;

    public StorageProcess(PublicAPI publicAPI, FileManager storage) {
        this.publicAPI = publicAPI;
        this.storage = storage;
    }

    public void run() {
        Date now = new Date();
        String fileName = Utils.fromDate(Utils.FORMAT, now) + ".csv";
        FearGreedIndex fearGreedIndex = publicAPI.getFearGreedIndex("1").get(0);
        Set<Exchange> processed = new HashSet<>();
        for (Exchange exchange : Exchange.values()) {
            try {
                if (exchange.enabled() && processed.add(exchange)) {
                    String lastPrice = "data" + exchange.getFolder() + Utils.LAST_PRICE;
                    Map<String, CsvRow> previousRows = storage.previousRows(lastPrice);
                    Map<String, Price> prices = exchange.price(publicAPI);
                    List<CsvRow> updatedRows = storage.updatedRowsAndSaveLastPrices(previousRows, prices, now,
                            lastPrice, fearGreedIndex.getValue());
                    StringBuilder builder = new StringBuilder();
                    for (CsvRow row : updatedRows) {
                        builder.append(row.toCsvLine());
                    }
                    storage.updateFile("data" + exchange.getFolder() + fileName,
                            builder.toString().getBytes(Utils.UTF8), Utils.CSV_ROW_HEADER.getBytes(Utils.UTF8));
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Cannot process " + exchange.name(), e);
            }
        }
    }
}
