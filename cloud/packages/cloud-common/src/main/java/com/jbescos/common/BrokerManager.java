package com.jbescos.common;

import java.io.IOException;
import java.util.List;

public interface BrokerManager {

	List<Broker> loadBrokers() throws IOException;
	
}
