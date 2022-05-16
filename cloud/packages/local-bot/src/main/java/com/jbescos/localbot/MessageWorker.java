package com.jbescos.localbot;

public interface MessageWorker<T> {

	boolean startToWork();
	
	void process(T message, long now);
	
}
