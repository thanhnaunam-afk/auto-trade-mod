package com.thanh.autotrade.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quét market 8 tiếng/lần, lưu giá median + supply của mỗi item.
 * Dùng cho Gemini API phân tích trend.
 */
public class MarketScanner {
    private static final Path HISTORY_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("market-history.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void recordMarketSnapshot(Map<String, ItemMarketData> itemPrices) {
        try {
            JsonObject history = loadHistory();
            String timestamp = LocalDateTime.now().format(FORMATTER);

            JsonArray snapshots = history.getAsJsonArray("snapshots");
            if (snapshots == null) {
                snapshots = new JsonArray();
                history.add("snapshots", snapshots);
            }

            JsonObject snapshot = new JsonObject();
            snapshot.addProperty("timestamp", timestamp);

            JsonArray items = new JsonArray();
            for (Map.Entry<String, ItemMarketData> entry : itemPrices.entrySet()) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("name", entry.getKey());
                itemObj.addProperty("median_price", entry.getValue().medianPrice);
                itemObj.addProperty("supply", entry.getValue().supply);
                itemObj.addProperty("margin_percent", entry.getValue().marginPercent);
                items.add(itemObj);
            }
            snapshot.add("items", items);

            snapshots.add(snapshot);

            // Giữ lại tối đa 30 snapshots (1 tháng nếu scan 1 lần/day)
            if (snapshots.size() > 30) {
                JsonArray trimmed = new JsonArray();
                for (int i = snapshots.size() - 30; i < snapshots.size(); i++) {
                    trimmed.add(snapshots.get(i));
                }
                history.add("snapshots", trimmed);
            }

            saveHistory(history);
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to record market snapshot: " + e.getMessage());
        }
    }

    private static JsonObject loadHistory() throws IOException {
        if (Files.exists(HISTORY_FILE)) {
            try (Reader r = Files.newBufferedReader(HISTORY_FILE, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                return obj != null ? obj : new JsonObject();
            }
        }
        return new JsonObject();
    }

    private static void saveHistory(JsonObject history) throws IOException {
        Files.createDirectories(HISTORY_FILE.getParent());
        try (Writer w = Files.newBufferedWriter(HISTORY_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(history, w);
        }
    }

    public static class ItemMarketData {
        public long medianPrice;
        public int supply;
        public double marginPercent;

        public ItemMarketData(long medianPrice, int supply, double marginPercent) {
            this.medianPrice = medianPrice;
            this.supply = supply;
            this.marginPercent = marginPercent;
        }
    }
}
