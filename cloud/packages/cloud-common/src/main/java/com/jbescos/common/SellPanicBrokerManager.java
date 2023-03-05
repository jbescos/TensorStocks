package com.jbescos.common;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.Broker.Action;

public class SellPanicBrokerManager extends DefaultBrokerManager {

    public SellPanicBrokerManager(CloudProperties cloudProperties, FileManager fileManager) {
        super(cloudProperties, fileManager);
    }

    @Override
    public List<Broker> loadBrokers() throws IOException {
        return loadBrokers(super.loadBrokers(), Action.SELL_PANIC);
    }

    public List<Broker> loadBrokers(List<Broker> brokers, Action action) throws IOException {
        return brokers.stream().map(broker -> new PanicBroker(broker.getSymbol(), broker.getNewest(),
                broker.getPreviousTransactions(), action)).collect(Collectors.toList());
    }

}
