package com.jbescos.common;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SellPanicBrokerManager extends DefaultBrokerManager {

    public SellPanicBrokerManager(CloudProperties cloudProperties, FileManager fileManager) {
        super(cloudProperties, fileManager);
    }

    @Override
    public List<Broker> loadBrokers() throws IOException {
        return super.loadBrokers().stream()
                .map(broker -> new PanicBroker(broker.getSymbol(), broker.getNewest(), broker.getPreviousTransactions()))
                .collect(Collectors.toList());
    }

}
