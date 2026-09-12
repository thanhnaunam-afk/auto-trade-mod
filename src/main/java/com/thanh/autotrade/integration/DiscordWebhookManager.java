package com.thanh.autotrade.integration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Discord Webhook Manager
 * Gửi log lên Discord channel
 */
public class DiscordWebhookManager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String webhookUrl;

    public DiscordWebhookManager(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Gửi simple message lên Discord
     */
    public void logEvent(String title, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            return; // Ko configure webhook → skip
        }

        try {
            JsonObject embed = new JsonObject();
            embed.addProperty("title", title);
            embed.addProperty("description", content);
            embed.addProperty("color", getColorByType(title));
            embed.addProperty("timestamp", LocalDateTime.now().toString());

            JsonObject payload = new JsonObject();
            payload.addProperty("content", "[" + LocalDateTime.now().format(TIME_FORMAT) + "]");

            com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            sendWebhook(payload);
        } catch (Exception e) {
            System.err.println("[AutoTrade] Discord webhook error: " + e.getMessage());
        }
    }

    /**
     * Log profit + balance
     */
    public void logProfit(long balance, long profit, String topItems) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            return;
        }

        try {
            JsonObject embed = new JsonObject();
            embed.addProperty("title", "💰 Balance Update");
            embed.addProperty("description", String.format(
                    "**Balance:** %,d\n**Profit:** %,d\n**Top Items:** %s",
                    balance, profit, topItems.isEmpty() ? "N/A" : topItems
            ));
            embed.addProperty("color", profit >= 0 ? 3066993 : 15158332); // Green or Red
            embed.addProperty("timestamp", LocalDateTime.now().toString());

            JsonObject payload = new JsonObject();
            payload.addProperty("content", "[" + LocalDateTime.now().format(TIME_FORMAT) + "]");

            com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            sendWebhook(payload);
        } catch (Exception e) {
            System.err.println("[AutoTrade] Discord webhook error: " + e.getMessage());
        }
    }

    /**
     * Log alert (loss, low margin, etc.)
     */
    public void logAlert(String itemName, String reason, long amount) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            return;
        }

        try {
            JsonObject embed = new JsonObject();
            embed.addProperty("title", "⚠️ Alert: " + itemName);
            embed.addProperty("description", String.format("%s: %,d", reason, amount));
            embed.addProperty("color", 15105570); // Orange

            JsonObject payload = new JsonObject();
            payload.addProperty("content", "[" + LocalDateTime.now().format(TIME_FORMAT) + "]");

            com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            sendWebhook(payload);
        } catch (Exception e) {
            System.err.println("[AutoTrade] Discord webhook error: " + e.getMessage());
        }
    }

    private void sendWebhook(JsonObject payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            System.err.println("[AutoTrade] Webhook error: " + response.statusCode() + " - " + response.body());
        }
    }

    private int getColorByType(String type) {
        if (type.contains("SUCCESS")) return 3066993; // Green
        if (type.contains("ERROR")) return 15158332; // Red
        if (type.contains("DETECT")) return 15105570; // Orange
        return 9807270; // Gray
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
