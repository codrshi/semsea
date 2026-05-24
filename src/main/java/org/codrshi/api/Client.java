package org.codrshi.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

//TODO: add retry logic
public abstract sealed class Client permits ChromaClient, OllamaClient, LLMClient {

    private final HttpClient httpClient;

    protected Client() {
        httpClient = HttpClient.newHttpClient();
    }

    protected HttpResponse<String> executePost(String url, String requestBody){

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(url))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return execute(request);
    }

    protected HttpResponse<String> executeDelete(String url){

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(url))
                .header("Content-type", "application/json")
                .DELETE()
                .build();

        return execute(request);
    }


    private HttpResponse<String> execute(HttpRequest request){
        HttpResponse<String> response;

        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response;
    }
}
