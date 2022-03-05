package com.jbescos.common;

public class PropertiesConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final TelegramInfo telegramInfo;

	public PropertiesConfigurationException(String message, TelegramInfo telegramInfo) {
		super(message);
		this.telegramInfo = telegramInfo;
	}

	public TelegramInfo getTelegramInfo() {
		return telegramInfo;
	}

}
