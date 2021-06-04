package com.jbescos.localbot;

import com.jbescos.localbot.WebSocket.Message;

public interface MessageWorker {

	boolean startToWork();
	
	void process(Message message, long now);
	
}
