package com.salaboy.knative.event.producer;

import com.salaboy.cloudevents.helper.CloudEventsHelper;
import io.cloudevents.CloudEvent;
import io.cloudevents.v03.AttributesImpl;
import io.cloudevents.v03.CloudEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Logger;

@SpringBootApplication
@RestController
public class KnativeEventProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnativeEventProducerApplication.class, args);
    }

    private final static Logger logger = Logger.getLogger(KnativeEventProducerApplication.class.getName());

    @Value("${SINK_HOST:localhost}")
    private String HOST;


    @GetMapping("/{name}")
    public String doSomethingAndSendCloudEvent(@PathVariable("name") String name) {

        final CloudEvent<AttributesImpl, String> myCloudEvent = CloudEventBuilder.<String>builder()
                .withId("ABC-123")
                .withType("my-first-cloud-event")
                .withSource(URI.create("knative-event-producer.default.svc.cluster.local"))
                .withData("{\"name\" : \"" + name + "-" + UUID.randomUUID().toString() + "\" }")
                .withDatacontenttype("application/json")
                .build();

        WebClient webClient = WebClient.builder().baseUrl(HOST).filter(logRequest()).build();

        WebClient.ResponseSpec postCloudEvent = CloudEventsHelper.createPostCloudEvent(webClient, myCloudEvent);

        postCloudEvent.bodyToMono(String.class).doOnError(t -> t.printStackTrace())
                .doOnSuccess(s -> System.out.println("Result -> " + s)).subscribe();

        return "OK!";

    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> logger.info(name + "=" + value)));
            return Mono.just(clientRequest);
        });
    }


}
