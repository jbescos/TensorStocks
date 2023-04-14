package com.jbescos.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

public class PublisherMgr implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(PublisherMgr.class.getName());
    private final Publisher publisher;

    public PublisherMgr(Publisher publisher) {
        this.publisher = publisher;
    }

    public void publish(String... messages) throws InterruptedException, ExecutionException, TimeoutException {
        List<ApiFuture<String>> responses = new ArrayList<>();
        for (String message : messages) {
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            LOGGER.info(() -> "Message sent: " + message);
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            responses.add(future);
        }
        for (ApiFuture<String> future : responses) {
            String response = future.get(10, TimeUnit.SECONDS);
            LOGGER.info(() -> "Response: " + response);
        }
    }

    @Override
    public void close() {
        publisher.shutdown();
    }

    public static PublisherMgr create(CloudProperties cloudProperties) throws IOException {
        TopicName topicName = TopicName.of(cloudProperties.PROJECT_ID, cloudProperties.GOOGLE_TOPIC_ID);
        return new PublisherMgr(Publisher.newBuilder(topicName).build());
    }
}
