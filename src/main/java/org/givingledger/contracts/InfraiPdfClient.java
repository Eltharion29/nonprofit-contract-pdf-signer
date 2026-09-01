package org.givingledger.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class InfraiPdfClient {
    private final InfraiConfig config;
    private final ObjectMapper json;
    private final HttpClient http;

    public InfraiPdfClient(InfraiConfig config, ObjectMapper json) {
        this.config = config;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public byte[] generateContract(String html, UUID idempotencyKey) throws IOException, InterruptedException {
        byte[] body = json.writeValueAsBytes(Map.of(
                "html", html,
                "page_size", "A4",
                "orientation", "portrait",
                "store", true));

        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            HttpRequest request = HttpRequest.newBuilder(config.baseUrl().resolve("/v1/pdf/generate"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode envelope = json.readTree(response.body());

            if (!envelope.path("ok").asBoolean(false)) {
                if (response.statusCode() == 429 && attempt < config.maxAttempts()) {
                    Thread.sleep(retryDelayMillis(response, attempt));
                    continue;
                }
                JsonNode error = envelope.path("error");
                throw new InfraiException(
                        error.path("code").asText("request_rejected"),
                        error.path("message").asText("PDF request rejected"),
                        response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("PDF transport failed with status " + response.statusCode());
            }

            URI download = URI.create(envelope.path("data").path("url").asText());
            HttpRequest downloadRequest = HttpRequest.newBuilder(download)
                    .timeout(Duration.ofSeconds(45))
                    .GET()
                    .build();
            HttpResponse<byte[]> pdf = http.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (pdf.statusCode() >= 400) {
                throw new IOException("Generated PDF download failed with status " + pdf.statusCode());
            }
            return pdf.body();
        }
        throw new IllegalStateException("retry loop exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiPdfClient::retryAfterMillis)
                .orElse(250L * (1L << (attempt - 1)));
    }

    private static long retryAfterMillis(String value) {
        try {
            return Math.max(0L, Long.parseLong(value) * 1000L);
        } catch (NumberFormatException ignored) {
            return 1000L;
        }
    }
}
