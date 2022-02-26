package com.jbescos.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
    private static final Map<String, List<String>> EXCHANGES_DATA = new HashMap<>();
    public final CloudProperties cloudProperties;
    private final Map<String, List<CsvRow>> grouped = new HashMap<>();
    private final String from;
    private final String to;
    
    static {
        File[] exchanges = listFiles("/data");
        for (File exchange : exchanges) {
        	File[] csvs = exchange.listFiles();
        	List<String> days = new ArrayList<>();
        	for (File csv : csvs) {
        	    if (!csv.getName().contains(Utils.LAST_PRICE) && csv.getName().endsWith(".csv")) {
        	        days.add(csv.getName());
        	    }
        	}
        	Collections.sort(days);
        	EXCHANGES_DATA.put(exchange.getName(), days);
        }
    }

    public DataLoader(String from, String to, CloudProperties cloudProperties) {
    	this.cloudProperties = cloudProperties;
    	this.from = from;
    	this.to = to;
    }
    
    public static String lastDayCsv(String exchange) {
    	List<String> csvs = EXCHANGES_DATA.get(exchange.toLowerCase());
    	if (csvs.isEmpty()) {
    		throw new IllegalArgumentException("There is no data for " + exchange);
    	}
    	return csvs.get(csvs.size() - 1).split("\\.")[0];
    }
    
    private static File[] listFiles(String resourcePath) {
    	URL dataResource = DataLoader.class.getResource(resourcePath);
    	String path = dataResource.getPath();
        File[] exchanges = new File(path).listFiles();
        return exchanges;
    }

    public void loadData() throws IOException {
        grouped.clear();
        List<CsvRow> rows = new ArrayList<>();
        List<String> resources = EXCHANGES_DATA.get(cloudProperties.USER_EXCHANGE.name().toLowerCase());
        boolean start = false;
        for (String resource : resources) {
            if (resource.endsWith(".csv")) {
                String date = resource.replace(".csv", "");
                if (date.equals(from)) {
                    start = true;
                } else if (date.equals(to)) {
                    break;
                }
                String csv = "/data" + cloudProperties.USER_EXCHANGE.getFolder() + resource;
                if (start) {
                    try {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(DataLoader.class.getResourceAsStream(csv)))) {
                            List<CsvRow> dailyRows = CsvUtil.readCsvRows(true, ",", reader, Collections.emptyList());
                            dailyRows = dailyRows.stream().filter(r -> cloudProperties.BOT_WHITE_LIST_SYMBOLS.isEmpty() || cloudProperties.BOT_WHITE_LIST_SYMBOLS.contains(r.getSymbol())).collect(Collectors.toList());
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
    	if (grouped.isEmpty()) {
    		throw new IllegalStateException("There is no data between " + from + " and " + to + " in " + cloudProperties.USER_EXCHANGE.name().toLowerCase());
    	}
        return grouped.get(symbol).get(0);
    }
    
    public CsvRow first() {
    	if (grouped.isEmpty()) {
    		throw new IllegalStateException("There is no data between " + from + " and " + to + " in " + cloudProperties.USER_EXCHANGE.name().toLowerCase());
    	}
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

	public CloudProperties getCloudProperties() {
		return cloudProperties;
	}
    
}
