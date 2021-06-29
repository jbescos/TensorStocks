package com.jbescos.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExchangeInfo {

    public static final String LOT_SIZE = "LOT_SIZE";
	private String timezone;
	private long serverTime;
	private Object[] rateLimits;
	private Object[] exchangeFilters;
	private List<Symbol> symbols = Collections.emptyList();

	public String getTimezone() {
		return timezone;
	}
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}
	public long getServerTime() {
		return serverTime;
	}
	public void setServerTime(long serverTime) {
		this.serverTime = serverTime;
	}
	public Object[] getRateLimits() {
		return rateLimits;
	}
	public void setRateLimits(Object[] rateLimits) {
		this.rateLimits = rateLimits;
	}
	public Object[] getExchangeFilters() {
		return exchangeFilters;
	}
	public void setExchangeFilters(Object[] exchangeFilters) {
		this.exchangeFilters = exchangeFilters;
	}
	public List<Symbol> getSymbols() {
        return symbols;
    }
    public void setSymbols(List<Symbol> symbols) {
        this.symbols = symbols;
    }
    public Map<String, Object> getFilter(String symbol, String filterName){
        for (Symbol sym : symbols) {
            if (sym.getSymbol().equals(symbol)) {
                List<Map<String, Object>> filters = sym.getFilters();
                for (Map<String, Object> filter : filters) {
                    Object obj = filter.get("filterType");
                    if (obj != null && obj.equals(filterName)) {
                        return filter;
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    public static class Symbol {
	    private String symbol;
	    private String status;
	    private String baseAsset;
	    private int baseAssetPrecision;
	    private String quoteAsset;
	    private int quotePrecision;
	    private int quoteAssetPrecision;
	    private int baseCommissionPrecision;
	    private int quoteCommissionPrecision;
	    private String[] orderTypes;
	    private boolean icebergAllowed;
	    private boolean ocoAllowed;
	    private boolean quoteOrderQtyMarketAllowed;
	    private boolean isSpotTradingAllowed;
	    private boolean isMarginTradingAllowed;
	    private String[] permissions;
	    private List<Map<String, Object>> filters = Collections.emptyList();
        public String getSymbol() {
            return symbol;
        }
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }
        public String getBaseAsset() {
            return baseAsset;
        }
        public void setBaseAsset(String baseAsset) {
            this.baseAsset = baseAsset;
        }
        public int getBaseAssetPrecision() {
            return baseAssetPrecision;
        }
        public void setBaseAssetPrecision(int baseAssetPrecision) {
            this.baseAssetPrecision = baseAssetPrecision;
        }
        public String getQuoteAsset() {
            return quoteAsset;
        }
        public void setQuoteAsset(String quoteAsset) {
            this.quoteAsset = quoteAsset;
        }
        public int getQuotePrecision() {
            return quotePrecision;
        }
        public void setQuotePrecision(int quotePrecision) {
            this.quotePrecision = quotePrecision;
        }
        public int getQuoteAssetPrecision() {
            return quoteAssetPrecision;
        }
        public void setQuoteAssetPrecision(int quoteAssetPrecision) {
            this.quoteAssetPrecision = quoteAssetPrecision;
        }
        public int getBaseCommissionPrecision() {
            return baseCommissionPrecision;
        }
        public void setBaseCommissionPrecision(int baseCommissionPrecision) {
            this.baseCommissionPrecision = baseCommissionPrecision;
        }
        public int getQuoteCommissionPrecision() {
            return quoteCommissionPrecision;
        }
        public void setQuoteCommissionPrecision(int quoteCommissionPrecision) {
            this.quoteCommissionPrecision = quoteCommissionPrecision;
        }
        public String[] getOrderTypes() {
            return orderTypes;
        }
        public void setOrderTypes(String[] orderTypes) {
            this.orderTypes = orderTypes;
        }
        public boolean isIcebergAllowed() {
            return icebergAllowed;
        }
        public void setIcebergAllowed(boolean icebergAllowed) {
            this.icebergAllowed = icebergAllowed;
        }
        public boolean isOcoAllowed() {
            return ocoAllowed;
        }
        public void setOcoAllowed(boolean ocoAllowed) {
            this.ocoAllowed = ocoAllowed;
        }
        public boolean isQuoteOrderQtyMarketAllowed() {
            return quoteOrderQtyMarketAllowed;
        }
        public void setQuoteOrderQtyMarketAllowed(boolean quoteOrderQtyMarketAllowed) {
            this.quoteOrderQtyMarketAllowed = quoteOrderQtyMarketAllowed;
        }
        public boolean isIsSpotTradingAllowed() {
            return isSpotTradingAllowed;
        }
        public void setIsSpotTradingAllowed(boolean isSpotTradingAllowed) {
            this.isSpotTradingAllowed = isSpotTradingAllowed;
        }
        public boolean isIsMarginTradingAllowed() {
            return isMarginTradingAllowed;
        }
        public void setIsMarginTradingAllowed(boolean isMarginTradingAllowed) {
            this.isMarginTradingAllowed = isMarginTradingAllowed;
        }
        public String[] getPermissions() {
            return permissions;
        }
        public void setPermissions(String[] permissions) {
            this.permissions = permissions;
        }
        public List<Map<String, Object>> getFilters() {
            return filters;
        }
        public void setFilters(List<Map<String, Object>> filters) {
            this.filters = filters;
        }
	}

}
