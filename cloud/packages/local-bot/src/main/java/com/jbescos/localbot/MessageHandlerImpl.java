package com.jbescos.localbot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.websocket.MessageHandler;

public class MessageHandlerImpl<T extends Symbolable> implements MessageHandler.Whole<String> {

    private static final Logger LOGGER = Logger.getLogger(MessageHandlerImpl.class.getName());
    private final ConcurrentHashMap<String, Long> symbolTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> symbolNotWorking = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageWorker<T>> symbolWorkers = new ConcurrentHashMap<>();
    private final Executor executor = Executors.newFixedThreadPool(Constants.WORKERS);
    private final Function<String, MessageWorker<T>> worker;
    private final Class<T> messageType;

    public MessageHandlerImpl(Class<T> messageType, Function<String, MessageWorker<T>> worker) {
        this.worker = worker;
        this.messageType = messageType;
    }

    @Override
    public void onMessage(String message) {
        try {
            T obj = Constants.MAPPER.readValue(message, messageType);
            final long now = System.currentTimeMillis();
            // It makes sure same symbol is not processed more than 1 time during the
            // Constants.LATENCY
            long result = symbolTimestamps.compute(obj.symbol(), (key, val) -> {
                if (val == null || (now - val > Constants.LATENCY)) {
                    return now;
                }
                return val;
            });
            if (now == result) {
                MessageWorker<T> worker = symbolWorkers.computeIfAbsent(obj.symbol(), k -> this.worker.apply(k));
                // Only one thread can work at the same time for each symbol
                boolean notWorking = symbolNotWorking.compute(obj.symbol(), (key, val) -> {
                    return worker.startToWork();
                });
                if (notWorking) {
                    executor.execute(() -> worker.process(obj, now));
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.fine("Couldn't parse " + message + " in " + messageType);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error", e);
        }
    }

}
