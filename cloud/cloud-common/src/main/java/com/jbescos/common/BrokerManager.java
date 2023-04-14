package com.jbescos.common;

import java.io.IOException;
import java.util.List;

import com.jbescos.exchange.Broker;

public interface BrokerManager {

    List<Broker> loadBrokers() throws IOException;

}
