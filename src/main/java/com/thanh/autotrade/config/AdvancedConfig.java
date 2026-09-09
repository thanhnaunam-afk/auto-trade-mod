package com.thanh.autotrade.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lưu Discord webhook URL, Gemini API key, account name.
 * File: config/autotrade-advanced.json
 */
public class AdvancedConfig {
    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("autotrade-advanced.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String discordWebhookUrl = "";
    public String geminiApiKey = "";
    public String accountName = "main";
    public boolean autoReconnectEnabled = true;
    public int autoReconnectDelaySec = 10;
    public int geminiAnalysisIntervalTicks = 576000; // 8 giờ
    public int detectionPayoutThreshold = 2; // số lần phát hiện trước khi tự /pay

    public static AdvancedConfig load() {
        if (!Files.exists(PATH)) {
            AdvancedConfig def = new AdvancedConfig();
            def.save();
            return def;
        }
        try (Reader r = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            AdvancedConfig cfg = GSON.fromJson(r, AdvancedConfig.class);
            return cfg != null ? cfg : new AdvancedConfig();
        } catch (IOException e) {
            return new AdvancedConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException ignored) {
        }
    }
}
