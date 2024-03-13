package com.jbescos.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.jbescos.exchange.Broker.Action;
import com.jbescos.exchange.CsvTransactionRow;
import com.jbescos.exchange.FileManager;
import com.jbescos.exchange.SecuredAPI;
import com.jbescos.exchange.Utils;

public class SecuredTestAPI implements SecuredAPI {
    
    private static final Logger LOGGER = Logger.getLogger(SecuredTestAPI.class.getName());
    private static final String INITIAL_USDT = "10000";
    private final FileManager storage;
    private final String walletFile;
    
    public SecuredTestAPI(CloudProperties cloudProperties, FileManager storage) {
        this.storage = storage;
        this.walletFile = cloudProperties.USER_ID + "/" + Utils.CONTEXT_WALLET_FILE;
    }
    
    @Override
    public Map<String, String> wallet() {
        Map<String, String> wallet = storage.loadWallet(walletFile);
        if (wallet == null || wallet.isEmpty()) {
            LOGGER.warning("No wallet found in " + walletFile);
            wallet = new LinkedHashMap<>();
            wallet.put(Utils.USDT, INITIAL_USDT);
        } else {
            wallet.remove(Utils.TOTAL_USDT);
        }
        LOGGER.info("Test wallet: " + wallet);
        return wallet;
    }

    @Override
    public CsvTransactionRow orderUSDT(String symbol, Action action, String quoteOrderQty, double currentUsdtPrice, boolean hasPreviousTransactions) {
        // It will be recalculated just after this
        return null;
    }

    @Override
    public CsvTransactionRow orderSymbol(String symbol, Action action, String quantity, double currentUsdtPrice, boolean hasPreviousTransactions) {
        // It will be recalculated just after this
        return null;
    }

    @Override
    public CsvTransactionRow synchronize(CsvTransactionRow precalculated) {
        throw new UnsupportedOperationException("Test does not support synchornize transactions");
    }

}
