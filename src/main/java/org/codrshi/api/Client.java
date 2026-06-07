package org.codrshi.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.metric.MetricType;
import org.codrshi.metric.Timer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

//TODO: add retry logic
public abstract sealed class Client permits ChromaClient, OllamaClient, LLMClient {

    private static final Logger log = LogManager.getLogger(Client.class);

    private final HttpClient httpClient;
    private final String serviceName;

    protected Client(String serviceName) {
        this.httpClient = HttpClient.newHttpClient();
        this.serviceName = serviceName;
    }

    protected HttpResponse<String> executePost(String url, String requestBody, MetricType metricType) {

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(url))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return execute(request, metricType);
    }

    protected HttpResponse<String> executeDelete(String url, MetricType metricType) {

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(url))
                .header("Content-type", "application/json")
                .DELETE()
                .build();

        return execute(request, metricType);
    }


    private HttpResponse<String> execute(HttpRequest request, MetricType metricType) {
        long startNanos = Timer.start();
        log.debug("{} -> {} {}", serviceName, request.method(), request.uri());
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = Timer.stop(startNanos) / 1_000_000;
            log.debug("{} <- {} {} status={} ({} ms)",
                    serviceName, request.method(), request.uri(), response.statusCode(), elapsedMs);
            MetricCollector.record(metricType, Timer.stop(startNanos));
            return response;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} request {} {} was interrupted", serviceName, request.method(), request.uri(), e);
            throw new SemseaException("The operation was interrupted.", e);
        }
        catch (IOException e) {
            log.error("{} request {} {} failed", serviceName, request.method(), request.uri(), e);
            int port = request.uri().getPort();
            String endpoint = port > 0
                    ? request.uri().getHost() + ":" + port
                    : request.uri().getHost();
            throw new SemseaException(
                    "Cannot reach " + serviceName + " at " + endpoint + ".",
                    "Make sure " + serviceName + " is running and reachable.",
                    e);
        }
    }
}
