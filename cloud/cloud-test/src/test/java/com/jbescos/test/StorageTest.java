package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.jbescos.common.FileStorage;
import com.jbescos.exchange.CsvProfitRow;

public class StorageTest {

    @Test
    public void loadProfitRows() throws URISyntaxException {
        URL resource = StorageTest.class.getResource("../../../");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        System.out.println("Base path is " + path);
        FileStorage storage = new FileStorage(path);
        List<CsvProfitRow> rows = storage.loadCsvProfitRows("/profit_2023-10.csv");
        assertEquals(14, rows.size());
    }
}
