package com.jbescos.exchange;

public class ClosePositionResponse {
    // {"position_id":"453","strategy_id":"113","open_timestamp":1634913995389,"close_timestamp":1634914090671,"open_price":"10.827000000000","close_price":"10.843000000000","base_asset":"UNFI","quote_asset":"USDT","size":0.1,"is_long":false}
    public int position_id;
    public int strategy_id;
    public long open_timestamp;
    public long close_timestamp;
    public String open_price;
    public String close_price;
    public String base_asset;
    public String quote_asset;
    public double size;
    public boolean is_long;

    @Override
    public String toString() {
        return "ClosePositionResponse [position_id=" + position_id + ", strategy_id=" + strategy_id
                + ", open_timestamp=" + open_timestamp + ", close_timestamp=" + close_timestamp + ", open_price="
                + open_price + ", close_price=" + close_price + ", base_asset=" + base_asset + ", quote_asset="
                + quote_asset + ", size=" + size + ", is_long=" + is_long + "]";
    }
}
