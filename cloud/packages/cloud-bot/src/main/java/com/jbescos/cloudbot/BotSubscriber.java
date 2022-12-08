package com.jbescos.cloudbot;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.jbescos.cloudbot.BotSubscriber.PubSubMessage;
import com.jbescos.common.BrokerManager;
import com.jbescos.common.BucketStorage;
import com.jbescos.common.CloudProperties;
import com.jbescos.common.CsvUtil;
import com.jbescos.common.DefaultBrokerManager;
import com.jbescos.common.StorageInfo;
import com.jbescos.common.TelegramBot;
import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.CsvTxSummaryRow;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.PublicAPI.News;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

//Entry: com.jbescos.cloudbot.BotSubscriber
public class BotSubscriber implements BackgroundFunction<PubSubMessage> {

    private static final Logger LOGGER = Logger.getLogger(BotSubscriber.class.getName());
    private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);

    @Override
    public void accept(PubSubMessage payload, Context context) throws Exception {
        long millis = System.currentTimeMillis();
        Date now = new Date(millis);
        String userId = new String(Base64.getDecoder().decode(payload.data));
        StorageInfo storageInfo = StorageInfo.build();
        Client client = ClientBuilder.newClient();
        CloudProperties cloudProperties = new CloudProperties(userId, storageInfo);
        PublicAPI publicAPI = new PublicAPI(client);
        BucketStorage bucketStorage = new BucketStorage(storageInfo);
        BrokerManager brokerManager = new DefaultBrokerManager(cloudProperties, bucketStorage);
        SecuredAPI securedApi = cloudProperties.USER_EXCHANGE.create(cloudProperties, client);
        List<Broker> brokers = brokerManager.loadBrokers();
        try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
            BotExecution bot = BotExecution.production(cloudProperties, securedApi, bucketStorage, telegram);
            bot.execute(brokers);

            // Update wallet in case the exchange supports it
            if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
                Map<String, String> wallet = securedApi.wallet();
                Map<String, Double> prices = Utils.simplePrices(cloudProperties.USER_EXCHANGE.price(publicAPI));
                List<Map<String, String>> rows = Utils.userUsdt(now, prices, wallet);
                bucketStorage.updateFile(
                        cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv",
                        CsvUtil.toString(rows).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
                String baseUsdt = Utils.baseUsdt(cloudProperties.FIXED_BUY.keySet(), rows);
                if (baseUsdt != null) {
                    bucketStorage.overwriteFile(cloudProperties.USER_ID + "/" + Utils.CONTEXT_DATA_FILE,
                            baseUsdt.getBytes(), null);
                }
            }
            Map<String, Double> benefits = Utils.calculateBenefits(brokers);
            LOGGER.info(() -> cloudProperties.USER_ID + ": Summary of benefits " + benefits);
            String body = CsvTxSummaryRow.toCsvBody(now, benefits);
            bucketStorage.updateFile(cloudProperties.USER_ID + "/" + Utils.TX_SUMMARY_PREFIX
                    + Utils.fromDate(Utils.FORMAT, now) + ".csv", body.getBytes(Utils.UTF8),
                    CsvTxSummaryRow.CSV_HEADER_TX_SUMMARY_TOTAL);

            // Synchronize transactions with the exchange
            if (cloudProperties.USER_EXCHANGE.isSupportSyncTransaction()) {
                // FIXME Use synchronized Utils.OPEN_POSSITIONS to update the other CSVs.
                ResyncTx resync = synchronizeTransactions(securedApi, bucketStorage, userId + "/" + Utils.OPEN_POSSITIONS);
                if (resync.needUpdate) {
                    List<String> txFiles = Utils.monthsBack(now, 2, userId + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
                    Map<String, CsvTransactionRow> byOrderId = new HashMap<>();
                    for (String txFile : txFiles) {
                        List<CsvTransactionRow> transactions = synchronizeTransactions(securedApi, bucketStorage, txFile).transactions;
                        transactions.stream().forEach(tx -> byOrderId.put(tx.getOrderId(), tx));
                    }
                    List<String> profitFiles = Utils.monthsBack(now, 2, userId + "/" + CsvProfitRow.PREFIX, ".csv");
                    for (String profitFile : profitFiles) {
                        List<CsvProfitRow> profitRows = bucketStorage.loadCsvProfitRows(profitFile);
                        boolean needUpdate = Utils.resyncProfit(byOrderId, profitRows, cloudProperties.BROKER_COMMISSION);
                        if (needUpdate) {
                            StringBuilder data = new StringBuilder();
                            profitRows.stream().forEach(profit -> {
                                data.append(profit.toCsvLine());
                            });
                            bucketStorage.overwriteFile(profitFile, data.toString().getBytes(Utils.UTF8),
                                    CsvProfitRow.HEADER.getBytes(Utils.UTF8));
                        }
                    }
                }
            }

            // Report
            boolean report = isReportTime(now, cloudProperties);
            if (report) {
                if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
                    telegram.sendHtmlLink();
                }
                List<CsvProfitRow> profitRows = bucketStorage.loadCsvProfitRows(userId, 2);
                StringBuilder profits = new StringBuilder().append("opened-closed, profit(%)");
                profits.append("\n").append(Utils.profitSummary(now, 1, profitRows));
                profits.append("\n").append(Utils.profitSummary(now, 7, profitRows));
                profits.append("\n").append(Utils.profitSummary(now, 30, profitRows));
                telegram.sendMessage(profits.toString());
            }
            news(cloudProperties, millis, publicAPI, telegram);
            
        }
        client.close();
        LOGGER.info(userId + ": function took " + ((System.currentTimeMillis() - millis) / 1000) + " seconds");
    }

    
    private void news(CloudProperties cloudProperties, long millis, PublicAPI publicAPI, TelegramBot telegram) {
        long minutes30Back = millis - (30 * 60 * 1000);
        Date rounded = Utils.dateRoundedTo10Min(new Date(minutes30Back));
        List<News> news = cloudProperties.USER_EXCHANGE.news(publicAPI, rounded.getTime());
        for (News n : news) {
            telegram.sendMessage(n.toString());
        }
    }
    
    private ResyncTx synchronizeTransactions(SecuredAPI securedApi, BucketStorage bucketStorage,
            String txFile) throws FileNotFoundException, IOException {
        List<CsvTransactionRow> transactions = bucketStorage.loadCsvTransactionRows(txFile);
        boolean needUpdate = Utils.resyncTransactions(securedApi, transactions);
        if (needUpdate) {
            StringBuilder data = new StringBuilder();
            transactions.stream().forEach(tx -> {
                data.append(tx.toCsvLine());
            });
            bucketStorage.overwriteFile(txFile, data.toString().getBytes(Utils.UTF8),
                    Utils.TX_ROW_HEADER.getBytes(Utils.UTF8));
        }
        return new ResyncTx(needUpdate, transactions);
    }

    // Reports if the bot run between 6:00 and 6:10
    private boolean isReportTime(Date now, CloudProperties cloudProperties) {
        if (cloudProperties.TELEGRAM_BOT_ENABLED) {
            return Utils.isTime(now, 6);
        }
        return false;
    }

    public static class PubSubMessage {
        String data;
        Map<String, String> attributes;
        String messageId;
        String publishTime;
    }
    
    private static class ResyncTx {
        private final boolean needUpdate;
        private final List<CsvTransactionRow> transactions;
        public ResyncTx(boolean needUpdate, List<CsvTransactionRow> transactions) {
            this.needUpdate = needUpdate;
            this.transactions = transactions;
        }
    }
}
