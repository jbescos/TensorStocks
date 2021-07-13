package com.jbescos.localbot;

import com.jbescos.localbot.WebSocket.Symbolable;

public interface MessageWorker<T extends Symbolable> {

	boolean startToWork();
	
	void process(T message, long now);
	
}
