package com.jbescos.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.jbescos.exchange.PublicAPI;

public class TokensProperties {

    public static void main(String[] args) throws IOException {
        Client client = ClientBuilder.newClient();
        PublicAPI api = new PublicAPI(client);
        Map<String, Map<String, String>> coinAddresses = api.coinAddresses();
        for (Entry<String, Map<String, String>> entry : coinAddresses.entrySet()) {
            String platform = entry.getKey();
            File properties = new File("target/" + platform + ".properties");
            properties.createNewFile();
            try (FileOutputStream out = new FileOutputStream(properties)) {
                for (Entry<String, String> pair : entry.getValue().entrySet()) {
                    String line = pair.getKey().replaceAll(" ", "_") + "=" + pair.getValue() + "\n";
                    out.write(line.getBytes("UTF-8"));
                }
                out.flush();
            }
        }
        // Rename properties to "polygon","ethereum","avalanche","aurora","arbitrum","bsc","rei"
    }

}
