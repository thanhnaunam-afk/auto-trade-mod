package com.thanh.autotrade.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gửi log và nhận commands qua Discord webhook.
 * - POST embed messages để log sự kiện (profit, respawn, disconnect...)
 * - Poll webhook history để đọc commands (!autotrade start, !join, !pay...)
 */
public class DiscordWebhookManager {
    private final String webhookUrl;
    private final String accountName;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ConcurrentLinkedQueue<DiscordMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public DiscordWebhookManager(String webhookUrl, String accountName) {
        this.webhookUrl = webhookUrl;
        this.accountName = accountName;
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            startMessageSender();
        }
    }

    public void logProfit(long balance, long profitToday, String top3Items) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "💰 " + accountName + " — Profit Report");
        embed.addProperty("color", 65280); // xanh lá cây

        JsonArray fields = new JsonArray();

        JsonObject balanceField = new JsonObject();
        balanceField.addProperty("name", "💳 Số dư");
        balanceField.addProperty("value", formatNumber(balance));
        balanceField.addProperty("inline", false);
        fields.add(balanceField);

        JsonObject profitField = new JsonObject();
        profitField.addProperty("name", "📈 Lợi nhuận hôm nay");
        profitField.addProperty("value", formatNumber(profitToday));
        profitField.addProperty("inline", false);
        fields.add(profitField);

        JsonObject top3Field = new JsonObject();
        top3Field.addProperty("name", "🎯 Top 3 item bán được");
        top3Field.addProperty("value", top3Items.isEmpty() ? "_(không có)_" : top3Items);
        top3Field.addProperty("inline", false);
        fields.add(top3Field);

        embed.add("fields", fields);
        embed.addProperty("timestamp", LocalDateTime.now().toString());

        sendEmbed(embed);
    }

    public void logStatus(String status) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "📊 Trạng thái — " + accountName);

        String color = status.contains("Chạy") ? "65280" :
                status.contains("Dừng") ? "16711680" :
                status.contains("Reconnect") ? "16776960" : "9807270";
        embed.addProperty("color", Integer.parseInt(color));

        JsonArray fields = new JsonArray();
        JsonObject statusField = new JsonObject();
        statusField.addProperty("name", "⏱️ Trạng thái");
        statusField.addProperty("value", status);
        statusField.addProperty("inline", false);
        fields.add(statusField);

        embed.add("fields", fields);
        embed.addProperty("timestamp", LocalDateTime.now().toString());

        sendEmbed(embed);
    }

    public void logEvent(String eventType, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        String emoji = switch (eventType) {
            case "RESPAWN" -> "🔄";
            case "DISCONNECT" -> "❌";
            case "RECONNECT" -> "🔌";
            case "PAYMENT" -> "💸";
            case "DETECT" -> "⚠️";
            case "ERROR" -> "🔥";
            default -> "ℹ️";
        };

        JsonObject embed = new JsonObject();
        embed.addProperty("title", emoji + " " + eventType);
        embed.addProperty("description", message);
        embed.addProperty("color", getEventColor(eventType));
        embed.addProperty("timestamp", LocalDateTime.now().toString());

        sendEmbed(embed);
    }

    public void logPayment(String targetPlayer, long amount) {
        logEvent("PAYMENT", "💸 [" + accountName + "] đã pay cho **" + targetPlayer + "**: **" + formatNumber(amount) + "**");
    }

    public void logDetection(int count) {
        logEvent("DETECT", "⚠️ Phát hiện lần " + count + " — đã thực hiện `/pay froglighter <all money>`");
    }

    private void sendEmbed(JsonObject embed) {
        JsonObject payload = new JsonObject();
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        messageQueue.offer(new DiscordMessage(payload.toString()));
    }

    private void startMessageSender() {
        executor.scheduleAtFixedRate(() -> {
            while (!messageQueue.isEmpty()) {
                DiscordMessage msg = messageQueue.poll();
                if (msg != null) {
                    sendMessage(msg.content);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void sendMessage(String jsonPayload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("[AutoTrade] Failed to send Discord webhook: " + e.getMessage());
        }
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%,.2fM", num / 1_000_000.0).replace(",", ".");
        } else if (num >= 1_000) {
            return String.format("%,.2fK", num / 1_000.0).replace(",", ".");
        }
        return String.format("%,d", num).replace(",", ".");
    }

    private int getEventColor(String eventType) {
        return switch (eventType) {
            case "RESPAWN" -> 16776960;     // vàng
            case "DISCONNECT" -> 16711680;   // đỏ
            case "RECONNECT" -> 65280;       // xanh
            case "PAYMENT" -> 16711680;      // đỏ (cảnh báo)
            case "DETECT" -> 16776960;       // vàng (cảnh báo)
            case "ERROR" -> 16711680;        // đỏ
            default -> 9807270;              // xám
        };
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private static class DiscordMessage {
        String content;

        DiscordMessage(String content) {
            this.content = content;
        }
    }
}
