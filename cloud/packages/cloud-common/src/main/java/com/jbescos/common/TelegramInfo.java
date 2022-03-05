package com.jbescos.common;

public class TelegramInfo {
	
	private final String userId;
	private final String token;
	private final String exceptionToken;
	private final String chatId;
	private final String chartBotUrl;
	private final boolean enabled;

	public TelegramInfo(boolean enabled, String userId, String token, String exceptionToken, String chatId,
			String chartBotUrl) {
		this.userId = userId;
		this.token = token;
		this.exceptionToken = exceptionToken;
		this.chatId = chatId;
		this.chartBotUrl = chartBotUrl;
		this.enabled = enabled;
	}

	public String getUserId() {
		return userId;
	}

	public String getToken() {
		return token;
	}

	public String getExceptionToken() {
		return exceptionToken;
	}

	public String getChatId() {
		return chatId;
	}

	public String getChartBotUrl() {
		return chartBotUrl;
	}

	public boolean isEnabled() {
		return enabled;
	}
}