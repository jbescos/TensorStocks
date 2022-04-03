package com.jbescos.test;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jbescos.common.Broker.Action;
import com.jbescos.common.CautelousBroker;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvRow;
import com.jbescos.common.CsvTransactionRow;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.TransactionsSummary;
import com.jbescos.common.Utils;

public class CautelousBrokerTest {

	@Test
	public void gensSell() throws IOException {
		CloudProperties cloudProperties = new CloudProperties("kucoin", null);
		try (BufferedReader csvReader = new BufferedReader(new InputStreamReader(CautelousBrokerTest.class.getResourceAsStream("/broker/GENSUSDT/GENSUSDT.csv")));
				BufferedReader txReader = new BufferedReader(new InputStreamReader(CautelousBrokerTest.class.getResourceAsStream("/broker/GENSUSDT/transactions.csv")));) {
			List<CsvRow> values = CsvUtil.readCsvRows(true, ",", csvReader, Collections.emptyList());
			List<CsvTransactionRow> previousTransactions = CsvUtil.readCsvTransactionRows(true, ",", txReader);
			TransactionsSummary summary = Utils.minSellProfitable(previousTransactions);
			CautelousBroker broker = new CautelousBroker(cloudProperties, "GENSUSDT", values, summary, false);
			assertEquals(summary.toString(), Action.SELL, broker.getAction());
		}
	}

	@Test
	public void ampl() throws IOException {
		try (BufferedReader txReader = new BufferedReader(new InputStreamReader(CautelousBrokerTest.class.getResourceAsStream("/broker/AMPLUSDT/transactions.csv")));) {
			List<CsvTransactionRow> previousTransactions = CsvUtil.readCsvTransactionRows(true, ",", txReader);
			List<CsvTransactionRow> reversedTransactions = new ArrayList<>(previousTransactions);
			Collections.reverse(reversedTransactions);
			assertEquals(Utils.minSellProfitable(previousTransactions).getLastPurchase(), Utils.minSellProfitable(reversedTransactions).getLastPurchase());
		}
	}
}
