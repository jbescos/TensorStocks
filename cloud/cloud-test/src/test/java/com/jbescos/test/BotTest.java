package com.jbescos.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;

import com.jbescos.cloudbot.BotUtils;
import com.jbescos.cloudbot.SymbolMinMax;
import com.jbescos.common.Utils;

public class BotTest {

	@Test
	public void dateToStart() throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(CsvUtilTest.class.getResourceAsStream("/predictions.csv")))) {
			List<SymbolMinMax> minMax = BotUtils.loadPredictions(Utils.fromString(Utils.FORMAT_SECOND, "2021-05-09 05:00:06"), reader);
			System.out.println(minMax);
		}
	}
}
