package com.jbescos.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import com.jbescos.common.CsvUtil;
import com.jbescos.common.TxSummaryBarChart;
import com.jbescos.exchange.IRow;

public class TxSummaryBarChartTest {

    @Test
    public void chart() throws IOException {
        File tmp = Files.createTempFile("TxSummaryBarChartTest", ".png").toFile();
        try (InputStream in = TxSummaryBarChartTest.class.getResourceAsStream("/tx_summary_2023-11-03.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                FileOutputStream out = new FileOutputStream(tmp)) {
            List<? extends IRow> rows = CsvUtil.readCsvTxSummaryRows(true, ",", reader);
            TxSummaryBarChart chart = new TxSummaryBarChart();
            chart.add("label", rows);
            chart.save(out);
        }
        System.out.println("Created in: " + tmp.getAbsolutePath());
    }
}
