package com.thanh.autotrade.market;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Market Scanner - scan AH data và lưu cache
 * Dùng cho margin analysis, Gemini AI, v.v.
 */
public class MarketScanner {
    private static final Path MARKET_CACHE = FabricLoader.getInstance()
            .getConfigDir().resolve("market-cache.json");
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class ItemMarketData {
        public String itemName;
        public long medianPrice;
        public long minPrice;
        public long maxPrice;
        public int supply;
        public int demand;
        public int transactionCount;
        public double margin;
        public LocalDateTime lastUpdated;

        public ItemMarketData(String itemName, long medianPrice, int supply, int demand) {
            this.itemName = itemName;
            this.medianPrice = medianPrice;
            this.supply = supply;
            this.demand = demand;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    private final Map<String, ItemMarketData> marketData = new ConcurrentHashMap<>();
    private int scanCountdown = 0;
    private static final int SCAN_INTERVAL = 12000; // 10 phút

    public void tick() {
        scanCountdown--;
        if (scanCountdown <= 0) {
            scanMarket();
            scanCountdown = SCAN_INTERVAL;
        }
    }

    private void scanMarket() {
        // TODO: Thực tế cần hook vào /ah GUI để lấy data
        // Tạm thời load từ cache
        loadMarketCache();
    }

    public void updateItemPrice(String itemName, long medianPrice, long minPrice, long maxPrice, 
                                 int supply, int demand, int transactionCount) {
        ItemMarketData data = new ItemMarketData(itemName, medianPrice, supply, demand);
        data.minPrice = minPrice;
        data.maxPrice = maxPrice;
        data.transactionCount = transactionCount;
        data.margin = calculateMargin(minPrice, maxPrice);
        marketData.put(itemName, data);
    }

    private double calculateMargin(long minPrice, long maxPrice) {
        if (minPrice == 0) return 0;
        return ((maxPrice - minPrice) * 100.0) / minPrice;
    }

    public ItemMarketData getItemData(String itemName) {
        return marketData.get(itemName);
    }

    public Map<String, ItemMarketData> getAllData() {
        return new HashMap<>(marketData);
    }

    public List<ItemMarketData> getTopItemsByMargin(int topN) {
        return marketData.values().stream()
                .sorted((a, b) -> Double.compare(b.margin, a.margin))
                .limit(topN)
                .toList();
    }

    public void saveMarketCache() {
        try {
            JsonObject root = new JsonObject();
            JsonArray itemsArray = new JsonArray();

            for (ItemMarketData data : marketData.values()) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("name", data.itemName);
                itemObj.addProperty("medianPrice", data.medianPrice);
                itemObj.addProperty("minPrice", data.minPrice);
                itemObj.addProperty("maxPrice", data.maxPrice);
                itemObj.addProperty("supply", data.supply);
                itemObj.addProperty("demand", data.demand);
                itemObj.addProperty("transactionCount", data.transactionCount);
                itemObj.addProperty("margin", data.margin);
                itemObj.addProperty("lastUpdated", data.lastUpdated.format(TIME_FORMAT));
                itemsArray.add(itemObj);
            }
            root.add("items", itemsArray);

            Files.createDirectories(MARKET_CACHE.getParent());
            Files.write(MARKET_CACHE, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to save market cache: " + e.getMessage());
        }
    }

    public void loadMarketCache() {
        if (!Files.exists(MARKET_CACHE)) return;

        try (Reader r = Files.newBufferedReader(MARKET_CACHE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root != null && root.has("items")) {
                JsonArray items = root.getAsJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    JsonObject itemObj = items.get(i).getAsJsonObject();
                    ItemMarketData data = new ItemMarketData(
                            itemObj.get("name").getAsString(),
                            itemObj.get("medianPrice").getAsLong(),
                            itemObj.get("supply").getAsInt(),
                            itemObj.get("demand").getAsInt()
                    );
                    data.minPrice = itemObj.get("minPrice").getAsLong();
                    data.maxPrice = itemObj.get("maxPrice").getAsLong();
                    data.transactionCount = itemObj.get("transactionCount").getAsInt();
                    data.margin = itemObj.get("margin").getAsDouble();
                    marketData.put(data.itemName, data);
                }
            }
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to load market cache: " + e.getMessage());
        }
    }
}
