package org.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrptApiTest {
    @Test
    public void call_thirty_times_with_three_calls_per_second_limit()  {
        CrptApi.CreateDocumentRequest request = new CrptApi.CreateDocumentRequest(
                new CrptApi.Description("111"),
                "dd",
                "ddd",
                "ddd",
                true,
                "dd",
                "dd",
                "dd",
                LocalDate.MAX,
                "",
                List.of(new CrptApi.Product(
                        "ddd",
                        LocalDate.now(),
                        "dd",
                        "ddd",
                        "dd",
                        LocalDate.now(),
                        "dd",
                        "ghgfg",
                        "fff")),
                LocalDate.MAX,
                "d"
        );
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);
        ExecutorService executorService = Executors.newCachedThreadPool();
        Instant start = Instant.now();
        Stream.generate(() -> toCompletableFuture(request, crptApi, executorService))
                .limit(30)
                .toList()
                .forEach(CompletableFuture::join);
        assertTrue(Instant.now().getEpochSecond() - start.getEpochSecond() > 9);
    }

    private static CompletableFuture<HttpResponse<String>> toCompletableFuture(CrptApi.CreateDocumentRequest request,
                                                                               CrptApi crptApi,
                                                                               ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return crptApi.createDocument(request);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }
}
