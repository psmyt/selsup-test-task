package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

public class CrptApi {
    private final URI uri = URI.create("https://bnk-wiremock.tusvc.t-global.bcs/tesssttt");
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    private final HttpClient client = HttpClient.newBuilder()
            .build();
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit < 1 || timeUnit == null) throw new IllegalArgumentException();
        this.semaphore = new Semaphore(requestLimit, true);
        long tick = timeUnit.toMillis(1) / requestLimit;
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(
                        () -> {
                            if (semaphore.availablePermits() < requestLimit) {
                                semaphore.release();
                            }
                        },
                        tick,
                        tick,
                        TimeUnit.MILLISECONDS
                );
    }

    public HttpResponse<String> createDocument(CreateDocumentRequest request) throws IOException, InterruptedException {
        semaphore.acquire();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(serialize(request)))
                .uri(uri)
                .build();
        return client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private String serialize(CreateDocumentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateDocumentRequest(
            Description description,
            String docId,
            String docStatus,
            String docType,
            @JsonProperty("importRequest") boolean importRequest,
            String ownerInn,
            String participantInn,
            String producerInn,
            LocalDate productionDate,
            String productionType,
            List<Product> products,
            LocalDate regDate,
            String regNumber) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Description(
            String participantInn
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Product(
            String certificateDocument,
            LocalDate certificateDocumentDate,
            String certificateDocumentNumber,
            String ownerInn,
            String producerInn,
            LocalDate productionDate,
            String tnvedCode,
            String uitCode,
            String uituCode
    ) {
    }
}
