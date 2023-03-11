package com.jbescos.common;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jbescos.exchange.Utils;

public interface IChart<T> {

    public static final String TITLE = "title";
    public static final String HORIZONTAL_LABEL = "horizontalLabel";
    public static final String VERTICAL_LABEL = "verticalLabel";
    public static final String WIDTH = "width";
    public static final String HEIGTH = "height";
    
    static final Color BACKGROUND_COLOR = new Color(0, 0, 0);

    void add(String lineLabel, List<? extends T> data);

    void save(OutputStream output) throws IOException;
    
    void property(String key, Object value);
    
    default Map<String, Object> defaultProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(WIDTH, 1920);
        properties.put(HEIGTH, 1080);
        properties.put(TITLE, "Crypto currencies");
        properties.put(HORIZONTAL_LABEL, "");
        properties.put(VERTICAL_LABEL, Utils.USDT);
        return properties;
    }
}
