package com.thanh.autotrade.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Gọi Gemini API (free tier) để phân tích market history.
 * Gợi ý item nào nên bán, cảnh báo duplicate item giữa các account.
 */
public class GeminiAnalyzer {
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String accountName;
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GeminiAnalyzer(String apiKey, String accountName) {
        this.apiKey = apiKey;
        this.accountName = accountName;
    }

    public void analyzeMarketAsync(DiscordWebhookManager webhook) {
        if (apiKey == null || apiKey.isBlank()) {
            if (webhook != null) webhook.logEvent("ERROR", "❌ Gemini API key chưa được cấu hình");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String marketHistory = readMarketHistory();
                String analysis = callGeminiAPI(marketHistory);
                if (webhook != null) {
                    webhook.logEvent("ANALYSIS", "🤖 Gemini phân tích: " + analysis);
                }
            } catch (Exception e) {
                if (webhook != null) {
                    webhook.logEvent("ERROR", "🔥 Lỗi gọi Gemini API: " + e.getMessage());
                }
            }
        });
    }

    private String readMarketHistory() throws IOException {
        Path historyFile = FabricLoader.getInstance()
                .getConfigDir().resolve("market-history.json");
        if (Files.exists(historyFile)) {
            return Files.readString(historyFile, StandardCharsets.UTF_8);
        }
        return "{}";
    }

    private String callGeminiAPI(String marketHistoryJson) throws IOException, InterruptedException {
        String prompt = buildPrompt(marketHistoryJson);

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();

        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);

        content.add("parts", parts);
        contents.add(content);

        requestBody.add("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(GEMINI_ENDPOINT + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
                    JsonArray parts2 = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                    if (parts2.size() > 0) {
                        return parts2.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
        }

        return "Không thể phân tích (API error: " + response.statusCode() + ")";
    }

    private String buildPrompt(String marketHistoryJson) {
        return "Hôm nay là " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ". " +
                "Phân tích dữ liệu giao dịch từ server DonutSMP:\n\n" +
                "=== MARKET HISTORY ===\n" +
                marketHistoryJson + "\n\n" +
                "=== ACCOUNT: " + accountName + " ===\n" +
                "Phân tích item nào có margin cao nhất (>= 8%), thanh khoản tốt (supply > 20). " +
                "Cảnh báo nếu item nào margin < 3% hoặc supply quá thấp. " +
                "Output ngắn gọn (tối đa 200 ký tự), chỉ gợi ý top 3 item nên bán.";
    }
}
