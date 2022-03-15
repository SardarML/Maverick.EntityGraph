package com.bechtle.eagl.graph.domain.services.scheduler;

import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.domain.services.AdminServices;
import config.TestConfigurations;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
class CheckGlobalIdentifiersTest {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ReplaceGlobalIdentifiers scheduled;

    @Autowired
    private AdminServices adminServices;


    @Test
    void checkForGlobalIdentifiers() {
        createEntities();

        Flux<Transaction> action = scheduled.checkForGlobalIdentifiers().doOnNext(transaction -> {
            log.trace("Completed transaction");
        });

        Duration duration = StepVerifier.create(action)
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .assertNext(transaction -> {
                    Assert.notNull(transaction, "transaction is null");
                })
                .verifyComplete();

        Assert.isTrue(duration.getNano() > 0, "no transaction time");


    }

    private void createEntities() {
        Resource file = new ClassPathResource("data/v1/requests/create-esco.ttl");

        Flux<DataBuffer> read = DataBufferUtils.read(file, new DefaultDataBufferFactory(), 128);

        Mono<Void> voidMono = adminServices.importEntities(read, "text/turtle");

        StepVerifier.create(voidMono).verifyComplete();
    }
}