package com.jbescos.exchange;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AllTickers {

    private String code;
    private Data data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private long time;
        private List<Map<String, Object>> ticker = Collections.emptyList();

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public List<Map<String, Object>> getTicker() {
            return ticker;
        }

        public void setTicker(List<Map<String, Object>> ticker) {
            this.ticker = ticker;
        }
    }
}
