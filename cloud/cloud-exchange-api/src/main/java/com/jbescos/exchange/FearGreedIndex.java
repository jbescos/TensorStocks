package com.jbescos.exchange;

import java.util.Date;

public class FearGreedIndex {

    private final String classification;
    private final int value;
    private final long timestamp;
    private final Date date;

    public FearGreedIndex(String classification, int value, long timestamp) {
        this.classification = classification;
        this.value = value;
        this.timestamp = timestamp;
        this.date = new Date(timestamp);
    }

    public String getClassification() {
        return classification;
    }

    public int getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "FearGreedIndex [classification=" + classification + ", value=" + value + ", timestamp=" + timestamp
                + ", date=" + date + "]";
    }

}
