package com.jbescos.localbot;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.websocket.DeploymentException;

public class Main {

	public static void main(String[] args) throws InterruptedException, DeploymentException, IOException, URISyntaxException {
		WebSocket socket = new WebSocket();
		socket.start();
		Thread.sleep(Long.MAX_VALUE);
	}

}
