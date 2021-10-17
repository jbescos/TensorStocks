package com.jbescos.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.Utils;

public class DataLoader {

    private static final Logger LOGGER = Logger.getLogger(DataLoader.class.getName());
    public static final CloudProperties CLOUD_PROPERTIES = new CloudProperties();
    private final Map<String, List<CsvRow>> grouped = new HashMap<>();

    public List<String> resources(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }
        return filenames;
    }

    private InputStream getResourceAsStream(String resource) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        return in == null ? DataLoader.class.getResourceAsStream(resource) : in;
    }

    public void loadData(String from, String to) throws IOException {
        grouped.clear();
        List<CsvRow> rows = new ArrayList<>();
        List<String> resources = resources("/");
        Collections.sort(resources);
        boolean start = false;
        for (String resource : resources) {
            if (resource.endsWith(".csv")) {
                String date = resource.replace(".csv", "");
                if (date.equals(from)) {
                    start = true;
                } else if (date.equals(to)) {
                    break;
                }
                if (start) {
                    try {
                        Utils.fromString(Utils.FORMAT, date);
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(DataLoader.class.getResourceAsStream("/" + resource)))) {
                            List<CsvRow> dailyRows = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
                            dailyRows = dailyRows.stream().filter(r -> CLOUD_PROPERTIES.BOT_WHITE_LIST_SYMBOLS.isEmpty() || CLOUD_PROPERTIES.BOT_WHITE_LIST_SYMBOLS.contains(r.getSymbol())).collect(Collectors.toList());
                            rows.addAll(dailyRows);
                        }
                    } catch (IllegalArgumentException e) {
                        // Not the file we want
                    }
                }
            }
        }
        grouped.putAll(rows.stream().sorted((r1, r2) -> r1.getDate().compareTo(r2.getDate())).collect(Collectors.groupingBy(CsvRow::getSymbol)));
    }
    
    public List<CsvRow> get(String symbol, long from, long to) {
        List<CsvRow> rows = grouped.get(symbol);
        return rows.stream().filter(row -> row.getDate().getTime() > from && row.getDate().getTime() <= to).collect(Collectors.toList());
    }
   
    public List<CsvRow> get(long from, long to) {
    	List<CsvRow> rows = new ArrayList<>();
    	for (String symbol : grouped.keySet()) {
    		rows.addAll(get(symbol, from, to));
    	}
    	return rows;
    }
    
    public List<CsvRow> get(String symbol) {
        List<CsvRow> rows = grouped.get(symbol);
        return rows.stream().collect(Collectors.toList());
    }
    
    public CsvRow first(String symbol) {
        return grouped.get(symbol).get(0);
    }
    
    public CsvRow first() {
        return grouped.entrySet().iterator().next().getValue().get(0);
    }

    public CsvRow next(String symbol, CsvRow current) {
        List<CsvRow> rows = grouped.get(symbol);
        int idx = rows.indexOf(current);
        if (idx < rows.size() - 2) {
            return rows.get(idx + 1);
        } else {
            return null;
        }
    }
    
    public CsvRow last(String symbol) {
        List<CsvRow> rows = grouped.get(symbol);
        return rows.get(rows.size() - 1);
    }

    public Collection<String> symbols() {
        return grouped.keySet();
    }
}
