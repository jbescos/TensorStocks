package com.jbescos.localbot;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.websocket.DeploymentException;

public interface IWebsocket {

    void start() throws DeploymentException, IOException, URISyntaxException;
}
