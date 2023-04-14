package com.jbescos.common;

public class TelegramInfo {

    private final String userId;
    private final String token;
    private final String chartReportId;
    private final String chatId;
    private final String chartBotUrl;

    public TelegramInfo(String userId, String token, String chartReportId, String chatId,
            String chartBotUrl) {
        this.userId = userId;
        this.token = token;
        this.chartReportId = chartReportId;
        this.chatId = chatId;
        this.chartBotUrl = chartBotUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public String getChatReportId() {
        return chartReportId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getChartBotUrl() {
        return chartBotUrl;
    }
}