package com.jbescos.exchange;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CsvContent {

    private final Set<String> categories = new LinkedHashSet<>();
    private final List<String> parsedLines = new ArrayList<>();

    // DATE,SYMBOL,PRICE
    public void add(String line) {
        String[] column = line.split(",");
        Date date = Utils.fromString(Utils.FORMAT_SECOND, column[0]);
        categories.add(column[1]);
        parsedLines.add((date.getTime() / 1000) + "," + column[1] + "," + column[2]);
    }

    public Set<String> getCategories() {
        return categories;
    }

    public List<String> getParsedLines() {
        return parsedLines;
    }

}
