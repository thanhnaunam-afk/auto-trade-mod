package com.thanh.autotrade.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thanh.autotrade.config.AdvancedConfig;
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
import java.util.*;

/**
 * Gemini Multi-Account Analyzer:
 * - Gợi ý item riêng cho mỗi account
 * - Tránh item trùng lặp giữa các account
 * - Scan AH 8h/lần để lấy data
 */
public class GeminiMultiAccountAnalyzer {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final Path ANALYSIS_CACHE = FabricLoader.getInstance()
            .getConfigDir().resolve("gemini-analysis-cache.json");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    private final AdvancedConfig config;
    private final Map<String, List<String>> accountItems = Collections.synchronizedMap(new HashMap<>());
    private String currentAccountName;
    private int analysisCountdown = 0;
    private static final int ANALYSIS_INTERVAL = 288000; // 8 giờ (288000 ticks)

    public GeminiMultiAccountAnalyzer(AdvancedConfig config, String accountName) {
        this.config = config;
        this.currentAccountName = accountName;
        this.analysisCountdown = ANALYSIS_INTERVAL;
        loadAnalysisCache();
    }

    public void tick() {
        analysisCountdown--;
        if (analysisCountdown <= 0) {
            analyzeAndGiveRecommendations();
            analysisCountdown = ANALYSIS_INTERVAL;
        }
    }

    /**
     * Phân tích AH data và gợi ý item tốt cho account này
     */
    private void analyzeAndGiveRecommendations() {
        try {
            String ahData = getAllAccountsMarketData();
            List<String> recommendations = callGeminiAnalysis(ahData);
            
            // Lọc item để tránh trùng lặp với account khác
            List<String> uniqueItems = filterUniqueItems(recommendations);
            accountItems.put(currentAccountName, uniqueItems);

            saveAnalysisCache();
            logAnalysis(uniqueItems);

        } catch (Exception e) {
            System.err.println("[AutoTrade] Gemini analysis failed: " + e.getMessage());
        }
    }

    /**
     * Gọi Gemini API với market data
     */
    private List<String> callGeminiAnalysis(String marketData) throws IOException, InterruptedException {
        String prompt = String.format(
                "Analyze the AH market data below for account '%s'. " +
                "Recommend top 5 items to farm/sell based on:\n" +
                "1. Margin percentage (profit = sell_price - cost)\n" +
                "2. Supply/demand ratio\n" +
                "3. Liquidity (transaction frequency)\n" +
                "4. Price stability (low volatility = safer)\n\n" +
                "Market Data:\n%s\n\n" +
                "Output ONLY item names, one per line. No explanations.",
                currentAccountName, marketData);

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

        String apiKey = config.geminiApiKey != null ? config.geminiApiKey : "";
        String url = GEMINI_API_URL + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API error: " + response.body());
        }

        return parseGeminiResponse(response.body());
    }

    private List<String> parseGeminiResponse(String response) {
        List<String> items = new ArrayList<>();
        try {
            JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    String text = parts.get(0).getAsJsonObject().get("text").getAsString();
                    for (String line : text.split("\n")) {
                        String item = line.trim();
                        if (!item.isEmpty()) {
                            items.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AutoTrade] Failed to parse Gemini response: " + e.getMessage());
        }
        return items;
    }

    /**
     * Lọc item để tránh trùng với account khác
     */
    private List<String> filterUniqueItems(List<String> recommendations) {
        List<String> uniqueItems = new ArrayList<>();
        Set<String> usedItems = new HashSet<>();

        // Collect items từ account khác
        for (Map.Entry<String, List<String>> entry : accountItems.entrySet()) {
            if (!entry.getKey().equals(currentAccountName)) {
                usedItems.addAll(entry.getValue());
            }
        }

        // Filter recommendations
        for (String item : recommendations) {
            if (!usedItems.contains(item)) {
                uniqueItems.add(item);
            }
        }

        return uniqueItems.isEmpty() ? recommendations : uniqueItems;
    }

    /**
     * Lấy market data từ tất cả account (simulate)
     */
    private String getAllAccountsMarketData() {
        // TODO: Thực tế cần lấy từ MarketScanner hoặc config
        // Tạm thời return mock data
        StringBuilder sb = new StringBuilder();
        sb.append("Item | Median Price | Supply | Demand\n");
        sb.append("Totem | 500 | 100 | 50\n");
        sb.append("Diamond | 1000 | 50 | 100\n");
        sb.append("Gold | 200 | 200 | 80\n");
        return sb.toString();
    }

    private void saveAnalysisCache() {
        try {
            JsonObject root = new JsonObject();
            JsonObject accountsObj = new JsonObject();

            for (Map.Entry<String, List<String>> entry : accountItems.entrySet()) {
                JsonArray itemsArray = new JsonArray();
                for (String item : entry.getValue()) {
                    itemsArray.add(item);
                }
                accountsObj.add(entry.getKey(), itemsArray);
            }

            root.add("accounts", accountsObj);
            root.addProperty("lastUpdated", LocalDateTime.now().format(TIME_FORMAT));

            Files.createDirectories(ANALYSIS_CACHE.getParent());
            Files.write(ANALYSIS_CACHE, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to save analysis cache: " + e.getMessage());
        }
    }

    private void loadAnalysisCache() {
        if (!Files.exists(ANALYSIS_CACHE)) return;

        try {
            String content = new String(Files.readAllBytes(ANALYSIS_CACHE), StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(content, JsonObject.class);
            JsonObject accounts = root.getAsJsonObject("accounts");

            for (String accountName : accounts.keySet()) {
                JsonArray items = accounts.getAsJsonArray(accountName);
                List<String> itemList = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    itemList.add(items.get(i).getAsString());
                }
                accountItems.put(accountName, itemList);
            }
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to load analysis cache: " + e.getMessage());
        }
    }

    private void logAnalysis(List<String> items) {
        String log = String.format("[%s] Account '%s' - Recommended items: %s\n",
                LocalDateTime.now().format(TIME_FORMAT), currentAccountName, items);
        System.out.println("[AutoTrade] " + log);
    }

    public List<String> getRecommendedItems() {
        return accountItems.getOrDefault(currentAccountName, new ArrayList<>());
    }

    public Map<String, List<String>> getAllAccountItems() {
        return new HashMap<>(accountItems);
    }
}
