package com.thanh.autotrade.safety;

import com.thanh.autotrade.integration.DiscordWebhookManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kiểm tra balance mỗi 3 phút qua `/balance` command
 * Log lợi nhuận lên Discord webhook
 */
public class BalanceChecker {
    private static final Path BALANCE_HISTORY = FabricLoader.getInstance()
            .getConfigDir().resolve("balance-history.json");
    private static final int CHECK_INTERVAL_TICKS = 3600; // 3 phút
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int checkCountdown = CHECK_INTERVAL_TICKS;
    private long lastBalance = 0;
    private long startDayBalance = 0;
    private long startDayProfit = 0;
    private DiscordWebhookManager webhook;

    public BalanceChecker(DiscordWebhookManager webhook) {
        this.webhook = webhook;
        this.startDayBalance = loadLastBalance();
    }

    public void tick() {
        checkCountdown--;
        if (checkCountdown <= 0) {
            // Mô phỏng lấy balance từ `/balance` command
            checkBalance();
            checkCountdown = CHECK_INTERVAL_TICKS;
        }
    }

    private void checkBalance() {
        // TODO: Thực tế cần hook vào chat message để lấy balance
        // Tạm thời dùng giá trị mô phỏng

        long currentBalance = getCurrentBalance();
        long profit = currentBalance - startDayBalance;

        // Log lên Discord
        if (webhook != null) {
            String top3Items = getTop3Items();
            webhook.logProfit(currentBalance, profit, top3Items);
        }

        saveBalance(currentBalance, profit);
        lastBalance = currentBalance;
    }

    private long getCurrentBalance() {
        // TODO: Hook vào chat message parser để lấy balance
        // Ví dụ: chat message "Your balance is: 10000000"
        return lastBalance;
    }

    private String getTop3Items() {
        // TODO: Lấy từ MultiOrderSystem hoặc MarketScanner
        // Tạm thời return empty
        return "";
    }

    private void saveBalance(long balance, long profit) {
        try {
            String log = String.format("[%s] Balance: %d | Profit: %d\n",
                    LocalDateTime.now().format(TIME_FORMAT), balance, profit);

            Files.createDirectories(BALANCE_HISTORY.getParent());
            Files.write(BALANCE_HISTORY, log.getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to save balance: " + e.getMessage());
        }
    }

    private long loadLastBalance() {
        try {
            if (Files.exists(BALANCE_HISTORY)) {
                String lastLine = Files.readAllLines(BALANCE_HISTORY).stream()
                        .reduce((a, b) -> b)
                        .orElse("");

                // Parse: "[HH:mm:ss] Balance: XXXX | Profit: YYYY"
                if (lastLine.contains("Balance:")) {
                    String[] parts = lastLine.split("Balance:")[1].trim().split("\\|")[0].trim().split(" ");
                    return Long.parseLong(parts[0]);
                }
            }
        } catch (Exception e) {
            System.err.println("[AutoTrade] Failed to load last balance: " + e.getMessage());
        }
        return 0;
    }

    public void setWebhook(DiscordWebhookManager webhook) {
        this.webhook = webhook;
    }

    public long getLastBalance() {
        return lastBalance;
    }
}
