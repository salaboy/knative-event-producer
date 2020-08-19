package com.salaboy.knative.event.producer;

import com.salaboy.cloudevents.helper.CloudEventsHelper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;


@SpringBootApplication
@RestController
@Slf4j
public class KnativeEventProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnativeEventProducerApplication.class, args);
    }


    @Value("${K_SINK:localhost}")
    private String HOST;


    @GetMapping("/{name}")
    public String doSomethingAndSendCloudEvent(@PathVariable("name") String name) {

        final CloudEvent myCloudEvent = CloudEventBuilder.v1()
                .withId("ABC-123")
                .withType("MyCloudEvent.JustHappened")
                .withSource(URI.create("knative-event-producer.default.svc.cluster.local"))
                .withData(SerializationUtils.serialize("{\"name\" : \"" + name + "-" + UUID.randomUUID().toString() + "\" }"))
                .withDataContentType("application/json")
                .build();

        WebClient webClient = WebClient.builder().baseUrl(HOST).filter(logRequest()).build();

        WebClient.ResponseSpec postCloudEvent = CloudEventsHelper.createPostCloudEvent(webClient, myCloudEvent);

        postCloudEvent.bodyToMono(String.class).doOnError(t -> t.printStackTrace())
                .doOnSuccess(s -> System.out.println("Result -> " + s)).subscribe();

        return "OK!";

    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(name + "=" + value)));
            return Mono.just(clientRequest);
        });
    }


}
