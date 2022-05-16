package com.jbescos.localbot;

import com.jbescos.localbot.PricesWorker.Price;

public interface Priceable extends Symbolable {

	Price toPrice();
}
