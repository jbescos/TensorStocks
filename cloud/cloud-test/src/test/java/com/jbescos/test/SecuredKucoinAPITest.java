package com.jbescos.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.SecuredKucoinAPI;
import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.Utils;

public class SecuredKucoinAPITest {

    private static final Logger LOGGER = Logger.getLogger(SecuredKucoinAPITest.class.getName());
    private static final CloudProperties CLOUD_PROPERTIES = new CloudProperties(null);
    private static final Client client = ClientBuilder.newClient();

    @BeforeClass
    public static void beforeClass() {
        client.register(new LoggingFeature(LOGGER, Level.INFO, null, null));
    }

    @AfterClass
    public static void afterClass() {
        client.close();
    }

    @Test
    @Ignore
    public void wallet() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        // {"code":"200000","data":[{"id":"61726237e6f0340001e0c85b","currency":"USDT","type":"trade","balance":"1000","available":"1000","holds":"0"}]}
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        System.out.println(api.wallet());
    }

    @Test
    @Ignore
    public void getOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        // Order Info: {id=618433b273c96c00019a6333, symbol=OPCT-USDT, opType=DEAL,
        // type=market, side=buy, price=0, size=2, funds=0, dealFunds=0.28224,
        // dealSize=2, fee=0.00028224, feeCurrency=USDT, stp=, stop=,
        // stopTriggered=false, stopPrice=0, timeInForce=GTC, postOnly=false,
        // hidden=false, iceberg=false, visibleSize=0, cancelAfter=0, channel=API,
        // clientOid=278bbdf4-d337-436f-9d58-637ac8874461, remark=null, tags=null,
        // isActive=false, cancelExist=false, createdAt=1636053938370, tradeType=TRADE}
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        System.out.println(api.getOrder("6184220f35ed8b00017a4940"));
    }

    @Test
    @Ignore
    public void limits() throws InvalidKeyException, NoSuchAlgorithmException {
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        System.out.println("KUCOIN: " + api.getSymbolLimits("FLAMEUSDT"));
    }

    @Test
    @Ignore
    public void address() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        PublicAPI publicAPI = new PublicAPI(client);
        Map<String, Double> kucoinSellPrices = Utils.simplePrices(publicAPI.priceKucoin());
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        System.out.println("Address:" + api.depositAddress("BTCUSDT", kucoinSellPrices));
    }

    @Test
    @Ignore
    public void depositAdresses() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        System.out.println("Address:" + api.depositAdresses("USDT"));
    }

    @Test
    @Ignore
    public void synchronize() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        List<CsvTransactionRow> transactions = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/resync2/zzzDaviX_transactions_transactions_2022-06.csv")))) {
            transactions = CsvUtil.readCsvTransactionRows(true, ",", reader);
        }
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        Utils.resyncTransactions(api, transactions);
        for (CsvTransactionRow tx : transactions) {
            System.out.print(tx.toCsvLine());
        }
        // Should be 7.16452306 of benefit
    }

    @Test
    @Ignore
    public void realOrder() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        SecuredKucoinAPI api = SecuredKucoinAPI.create(CLOUD_PROPERTIES, client);
        CsvTransactionRow row = api.orderSymbol("MKRUSDT", Action.SELL, "1.07673363", 978.15866617, false);
        System.out.println(row);
    }
}
