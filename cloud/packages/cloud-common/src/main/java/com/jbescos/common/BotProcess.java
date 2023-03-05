package com.jbescos.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvProfitRow;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.CsvTxSummaryRow;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.PublicAPI;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;

public class BotProcess {

    private static final Logger LOGGER = Logger.getLogger(BotProcess.class.getName());
    private static final byte[] CSV_HEADER_ACCOUNT_TOTAL = "DATE,SYMBOL,SYMBOL_VALUE,USDT\r\n".getBytes(Utils.UTF8);
    private final CloudProperties cloudProperties;
    private final Client client;
    private final FileManager bucketStorage;

    public BotProcess(CloudProperties cloudProperties, Client client, FileManager bucketStorage) {
        this.cloudProperties = cloudProperties;
        this.client = client;
        this.bucketStorage = bucketStorage;
    }

    public void execute() throws Exception {
        long millis = System.currentTimeMillis();
        Date now = new Date(millis);
        String userId = cloudProperties.USER_ID;
        PublicAPI publicAPI = new PublicAPI(client);
        BrokerManager brokerManager = new DefaultBrokerManager(cloudProperties, bucketStorage);
        SecuredAPI securedApi = cloudProperties.USER_EXCHANGE.create(cloudProperties, client, bucketStorage);
        List<Broker> brokers = brokerManager.loadBrokers();
        List<Map<String, String>> rowsWallet = Collections.emptyList();
        try (TelegramBot telegram = new TelegramBot(cloudProperties, client)) {
            BotExecution bot = BotExecution.production(cloudProperties, securedApi, bucketStorage, telegram);
            bot.execute(brokers);

            // Update wallet in case the exchange supports it
            if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
                Map<String, String> wallet = securedApi.wallet();
                Map<String, Double> prices = Utils.simplePrices(cloudProperties.USER_EXCHANGE.price(publicAPI));
                rowsWallet = Utils.userUsdt(now, prices, wallet);
                bucketStorage.updateFile(
                        cloudProperties.USER_ID + "/" + Utils.WALLET_PREFIX + Utils.thisMonth(now) + ".csv",
                        CsvUtil.toString(rowsWallet).toString().getBytes(Utils.UTF8), CSV_HEADER_ACCOUNT_TOTAL);
                String baseUsdt = Utils.baseUsdt(cloudProperties.FIXED_BUY.keySet(), rowsWallet);
                if (baseUsdt != null) {
                    bucketStorage.overwriteFile(cloudProperties.USER_ID + "/" + Utils.CONTEXT_DATA_FILE,
                            baseUsdt.getBytes(), null);
                }
            }
            calculateBenefits(now, brokers);
            // Synchronize transactions with the exchange
            synchronizeWithExchange(now, securedApi);
            // Report
            report(now, telegram, rowsWallet);
        }
        LOGGER.info(userId + ": function took " + ((System.currentTimeMillis() - millis) / 1000) + " seconds");
    }

    private void calculateBenefits(Date now, List<Broker> brokers) throws FileNotFoundException, IOException {
        Map<String, Double> benefits = Utils.calculateBenefits(brokers);
        LOGGER.info(() -> cloudProperties.USER_ID + ": Summary of benefits " + benefits);
        String body = CsvTxSummaryRow.toCsvBody(now, benefits);
        bucketStorage.updateFile(cloudProperties.USER_ID + "/" + Utils.TX_SUMMARY_PREFIX
                + Utils.fromDate(Utils.FORMAT, now) + ".csv", body.getBytes(Utils.UTF8),
                CsvTxSummaryRow.CSV_HEADER_TX_SUMMARY_TOTAL);
    }
    
    private void synchronizeWithExchange(Date now, SecuredAPI securedApi) throws FileNotFoundException, IOException {
        if (cloudProperties.USER_EXCHANGE.isSupportSyncTransaction()) {
            // FIXME Use synchronized Utils.OPEN_POSSITIONS to update the other CSVs.
            ResyncTx resync = synchronizeTransactions(securedApi, cloudProperties.USER_ID + "/" + Utils.OPEN_POSSITIONS);
            if (resync.needUpdate) {
                List<String> txFiles = Utils.monthsBack(now, 2, cloudProperties.USER_ID + "/" + Utils.TRANSACTIONS_PREFIX, ".csv");
                Map<String, CsvTransactionRow> byOrderId = new HashMap<>();
                for (String txFile : txFiles) {
                    List<CsvTransactionRow> transactions = synchronizeTransactions(securedApi, txFile).transactions;
                    transactions.stream().forEach(tx -> byOrderId.put(tx.getOrderId(), tx));
                }
                List<String> profitFiles = Utils.monthsBack(now, 2, cloudProperties.USER_ID + "/" + CsvProfitRow.PREFIX, ".csv");
                for (String profitFile : profitFiles) {
                    List<CsvProfitRow> profitRows = bucketStorage.loadCsvProfitRows(profitFile);
                    boolean needUpdate = Utils.resyncProfit(byOrderId, profitRows,
                            cloudProperties.BROKER_COMMISSION);
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
    }
    
    private void report(Date now, TelegramBot telegram, List<Map<String, String>> rowsWallet) throws IOException {
        boolean report = isReportTime(now);
        if (report) {
            if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
                telegram.sendHtmlLink();
            }
            StringBuilder message = new StringBuilder();
            List<CsvProfitRow> profitRows = bucketStorage.loadCsvProfitRows(cloudProperties.USER_ID, 4);
            message.append("ℹ️ Daily report ℹ️");
            message.append("📈 Opened-Closed($), Profit(%)");
            message.append("\n").append(Utils.profitSummary(now, 1, profitRows));
            message.append("\n").append(Utils.profitSummary(now, 7, profitRows));
            message.append("\n").append(Utils.profitSummary(now, 30, profitRows));
            message.append("\n").append(Utils.profitSummary(now, 90, profitRows));
            List<CsvTransactionRow> transactions = bucketStorage.loadOpenTransactions(cloudProperties.USER_ID);
            message.append("\n⏲️ Pending open positions: ").append(transactions.size());
            List<CsvProfitRow> dailyProfits = Utils.profitsDaysBack(now, 1, profitRows);
            message.append("\n📜 Closed positions:");
            message.append("\nHour, Symbol, Count, Profit(%)");
            for (CsvProfitRow profit : dailyProfits) {
                int count = profit.getBuyIds().split(Utils.ORDER_ID_SPLIT).length;
                message.append("\n ").append(Utils.fromDate(Utils.FORMAT_HOUR, profit.getSellDate())).append(", ").append(profit.getSymbol()).append(", ").append(count).append(", ").append(profit.getProfitPercentage());
            }
            if (cloudProperties.USER_EXCHANGE.isSupportWallet()) {
                message.append("\n💰 Wallet:");
                for (Map<String, String> entry : rowsWallet) {
                    String symbol = entry.get("SYMBOL");
                    String usdt = entry.get(Utils.USDT);
                    message.append("\n ").append(symbol).append(": ").append(Utils.format(Double.parseDouble(usdt), 2)).append("$");
                }
            }
            telegram.sendMessage(message.toString());
        }
    }
    
    private ResyncTx synchronizeTransactions(SecuredAPI securedApi, String txFile)
            throws FileNotFoundException, IOException {
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
    private boolean isReportTime(Date now) {
        return Utils.isTime(now, 6);
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
