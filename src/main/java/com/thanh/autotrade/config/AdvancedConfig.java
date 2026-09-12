package com.thanh.autotrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Advanced config cho Phase 2: Discord webhook, Gemini API key, etc.
 */
public class AdvancedConfig {
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("autotrade-advanced.json");
    private static final Gson GSON = new Gson();

    public String discordWebhookUrl = "https://discord.com/api/webhooks/YOUR_WEBHOOK_HERE";
    public String geminiApiKey = "YOUR_GEMINI_API_KEY";
    public double marginThresholdGood = 0.08; // 8%
    public double marginThresholdOk = 0.05; // 5%
    public double marginThresholdLow = 0.03; // 3%
    public int detectionRadius = 10;
    public int scanIntervalMinutes = 10;
    public int balanceCheckIntervalMinutes = 3;

    public void load() throws IOException {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader r = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(r, JsonObject.class);
                if (json != null) {
                    discordWebhookUrl = json.get("discordWebhookUrl").getAsString();
                    geminiApiKey = json.get("geminiApiKey").getAsString();
                    marginThresholdGood = json.get("marginThresholdGood").getAsDouble();
                    marginThresholdOk = json.get("marginThresholdOk").getAsDouble();
                    marginThresholdLow = json.get("marginThresholdLow").getAsDouble();
                    detectionRadius = json.get("detectionRadius").getAsInt();
                }
            }
        } else {
            save();
        }
    }

    public void save() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("discordWebhookUrl", discordWebhookUrl);
        json.addProperty("geminiApiKey", geminiApiKey);
        json.addProperty("marginThresholdGood", marginThresholdGood);
        json.addProperty("marginThresholdOk", marginThresholdOk);
        json.addProperty("marginThresholdLow", marginThresholdLow);
        json.addProperty("detectionRadius", detectionRadius);

        Files.createDirectories(CONFIG_FILE.getParent());
        try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(json, w);
        }
    }
}
