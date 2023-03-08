package com.jbescos.common;

import java.util.List;
import java.util.logging.Logger;

import com.jbescos.exchange.Broker;
import com.jbescos.exchange.CsvRow;
import com.jbescos.exchange.TransactionsSummary;

public class PanicBroker implements Broker {

    private static final Logger LOGGER = Logger.getLogger(PanicBroker.class.getName());
    private final String symbol;
    private final CsvRow newest;
    private final TransactionsSummary summary;
    private final Action specified;
    private final List<CsvRow> values;
    private Action action;

    public PanicBroker(String symbol, CsvRow newest, TransactionsSummary summary, List<CsvRow> values, Action specified) {
        this.symbol = symbol;
        this.newest = newest;
        this.summary = summary;
        this.specified = specified;
        this.values = values;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public CsvRow getNewest() {
        return newest;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public double getFactor() {
        return 0.999;
    }

    @Override
    public TransactionsSummary getPreviousTransactions() {
        return summary;
    }

    @Override
    public void evaluate(double benefitsAvg) {
        if (!summary.isHasTransactions() && (action == Action.SELL || action == Action.SELL_PANIC)) {
            this.action = Action.NOTHING;
        } else {
            this.action = specified;
        }
    }

    @Override
    public List<CsvRow> getValues() {
        return values;
    }

}
