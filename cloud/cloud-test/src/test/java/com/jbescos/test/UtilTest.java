package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.jbescos.common.Utils;

public class UtilTest {

	@Test
	public void daysBack() {
		Date from = Utils.fromString(Utils.FORMAT, "2020-01-02");
		List<String> days = Utils.daysBack(from, 4, "", ".csv");
		assertEquals(Arrays.asList("2019-12-30.csv", "2019-12-31.csv", "2020-01-01.csv", "2020-01-02.csv"), days);
	}
}
