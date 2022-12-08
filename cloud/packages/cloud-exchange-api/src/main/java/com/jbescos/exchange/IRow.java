package com.jbescos.exchange;

import java.util.Date;

public interface IRow {

    Date getDate();

    double getPrice();

    String getLabel();

    Double getAvg();

    Double getAvg2();
}
