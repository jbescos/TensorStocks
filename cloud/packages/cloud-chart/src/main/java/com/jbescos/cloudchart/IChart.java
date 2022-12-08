package com.jbescos.cloudchart;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface IChart<T> {

    static final Color BACKGROUND_COLOR = new Color(0, 0, 0);

    void add(String lineLabel, List<? extends T> data);

    void save(OutputStream output, String title, String horizontalLabel, String verticalLabel) throws IOException;
}
