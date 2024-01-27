package com.alexeyanufriev.rabbitimmediateack;

import com.jayway.jsonpath.JsonPath;
import org.apache.tomcat.util.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@SpringBootTest
@Testcontainers
class ImmediateAckTest {

    @Container
    public static final RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void testMessageImmediateAck() throws InterruptedException {
        this.rabbitTemplate.convertAndSend("test-queue", UUID.randomUUID().toString());

        // give rabbit a time to consume the message
        // and let management plugin reflect the queue state
        Thread.sleep(5000);

        int messagesInQueue = getQueueMessagesCount();

        Assertions.assertThat(messagesInQueue)
                .withFailMessage("`ImmediateAcknowledgeAmqpException` should evict message from the queue")
                .isZero();
    }

    static Integer getQueueMessagesCount() {
        String rabbitMgmtUrl = "http://" + rabbitMQContainer.getHost() + ":" + rabbitMQContainer.getMappedPort(15672);
        String rabbitQueueInfoUrl = rabbitMgmtUrl + "/api/queues/%2F/test-queue";

        String credentials = new String(Base64.encodeBase64("guest:guest".getBytes(), false));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rabbitQueueInfoUrl))
                .header("Authorization", "Basic " + credentials)
                .build();

        HttpClient client = HttpClient.newHttpClient();
        String queueState = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).join();

        return JsonPath.parse(queueState).read("$['messages_unacknowledged']");
    }

    @DynamicPropertySource
    static void rabbitMQConfig(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        dynamicPropertyRegistry.add("spring.rabbitmq.port", () -> rabbitMQContainer.getMappedPort(5672));
    }

}
