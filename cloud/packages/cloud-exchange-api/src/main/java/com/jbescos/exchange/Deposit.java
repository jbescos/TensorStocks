package com.jbescos.exchange;

import java.util.logging.Logger;

public class Deposit {

    private static final Logger LOGGER = Logger.getLogger(Deposit.class.getName());
    private final String currency;
    private final boolean withdrawEnabled;
    private final String withdrawMinFee;
    private final String remainAmount;
    private final String withdrawMinSize;
    private final String chain;
    private String address;
    private String remainAmountUsd;
    private String withdrawMinFeeUsd;
    private String withdrawMinSizeUsd;
    private String usdUnit;
    private double calculatedTotal;
    private double calculatedAfterCommissionTotal;
    private double calculatedTotalUsd;
    private double calculatedAfterCommissionTotalUsd;
    private String memo;

    public Deposit(String currency, boolean withdrawEnabled, String withdrawMinFee, String remainAmount,
            String withdrawMinSize, String chain) {
        this.currency = currency;
        this.withdrawEnabled = withdrawEnabled;
        this.withdrawMinFee = withdrawMinFee;
        this.remainAmount = remainAmount;
        this.withdrawMinSize = withdrawMinSize;
        this.chain = chain;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isWithdrawEnabled() {
        return withdrawEnabled;
    }

    public String getWithdrawMinFee() {
        return withdrawMinFee;
    }

    public String getRemainAmount() {
        return remainAmount;
    }

    public String getWithdrawMinSize() {
        return withdrawMinSize;
    }

    public String getWithdrawMinFeeUsd() {
        return withdrawMinFeeUsd;
    }

    public void setWithdrawMinFeeUsd(String withdrawMinFeeUsd) {
        this.withdrawMinFeeUsd = withdrawMinFeeUsd;
    }

    public String getRemainAmountUsd() {
        return remainAmountUsd;
    }

    public void setRemainAmountUsd(String remainAmountUsd) {
        this.remainAmountUsd = remainAmountUsd;
    }

    public String getWithdrawMinSizeUsd() {
        return withdrawMinSizeUsd;
    }

    public void setWithdrawMinSizeUsd(String withdrawMinSizeUsd) {
        this.withdrawMinSizeUsd = withdrawMinSizeUsd;
    }

    public String getChain() {
        return chain;
    }

    public String getUsdUnit() {
        return usdUnit;
    }

    public void setUsdUnit(String usdUnit) {
        this.usdUnit = usdUnit;
    }

    public double getCalculatedTotalUsd() {
        return calculatedTotalUsd;
    }

    public double getCalculatedAfterCommissionTotalUsd() {
        return calculatedAfterCommissionTotalUsd;
    }

    public double getCalculatedTotal() {
        return calculatedTotal;
    }

    public double getCalculatedAfterCommissionTotal() {
        return calculatedAfterCommissionTotal;
    }

    public String getMemo() {
        return memo;
    }

    @Override
    public String toString() {
        return "Deposit [currency=" + currency + ", withdrawEnabled=" + withdrawEnabled + ", withdrawMinFee="
                + withdrawMinFee + ", remainAmount=" + remainAmount + ", withdrawMinSize=" + withdrawMinSize
                + ", chain=" + chain + ", address=" + address + ", remainAmountUsd=" + remainAmountUsd
                + ", withdrawMinFeeUsd=" + withdrawMinFeeUsd + ", withdrawMinSizeUsd=" + withdrawMinSizeUsd
                + ", usdUnit=" + usdUnit + "]";
    }

    public void calculate(double amount) {
        this.calculatedTotal = amount;
        this.calculatedAfterCommissionTotal = amount - Double.parseDouble(withdrawMinFee);
        double usdUnit = Double.parseDouble(this.usdUnit);
        this.calculatedTotalUsd = Utils.usdValue(amount, usdUnit);
        // FIXME Not sure if after commission is calculated in this way
        this.calculatedAfterCommissionTotalUsd = this.calculatedTotalUsd - Double.parseDouble(this.withdrawMinFeeUsd);
        this.memo = "Transfer " + Utils.format(amount) + currency + " = " + Utils.format(calculatedTotalUsd)
                + "$. After commission is " + Utils.format(calculatedAfterCommissionTotal) + currency + "="
                + Utils.format(calculatedAfterCommissionTotalUsd) + "$";
        LOGGER.info(memo);
    }
}
