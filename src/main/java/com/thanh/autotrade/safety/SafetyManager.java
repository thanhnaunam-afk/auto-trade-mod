package com.thanh.autotrade.safety;

import com.thanh.autotrade.integration.DiscordWebhookManager;
import com.thanh.autotrade.trade.MultiOrderSystem;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Safety Manager:
 * - Kiểm tra profit so với order cost
 * - Nếu profit < cost → DỪNG BÁN item đó (tránh lỗ)
 * - Alert Discord khi bị lỗ
 * - Margin check: alert nếu < 3%
 */
public class SafetyManager {
    private static final Path SAFETY_LOG = FabricLoader.getInstance()
            .getConfigDir().resolve("safety-alerts.log");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MultiOrderSystem orderSystem;
    private final DiscordWebhookManager webhook;
    private final Set<String> stoppedItems = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Long> itemCostBasis = Collections.synchronizedMap(new HashMap<>());

    public SafetyManager(MultiOrderSystem orderSystem, DiscordWebhookManager webhook) {
        this.orderSystem = orderSystem;
        this.webhook = webhook;
    }

    /**
     * Kiểm tra profit trước khi bán item
     * Trả về true nếu ĐƯỢC BÁN, false nếu DỪNG BÁN
     */
    public boolean canSellItem(String itemName, long currentPrice, long quantity) {
        // Nếu item đã trong danh sách dừng → ko bán
        if (stoppedItems.contains(itemName)) {
            return false;
        }

        // Lấy cost basis từ order đầu tiên
        List<MultiOrderSystem.Order> orders = orderSystem.getOrdersByItem(itemName);
        if (orders.isEmpty()) {
            return true; // Ko có order → có thể bán AH
        }

        long costPerUnit = orders.get(0).pricePerUnit;
        long totalCost = costPerUnit * quantity;
        long totalProfit = (currentPrice - costPerUnit) * quantity;

        // Kiểm tra lỗ
        if (totalProfit < 0) {
            stopSellingItem(itemName, currentPrice, costPerUnit, totalProfit);
            return false;
        }

        // Kiểm tra margin
        double marginPercent = (totalProfit * 100.0) / totalCost;
        if (marginPercent < 3.0) {
            alertLowMargin(itemName, marginPercent, currentPrice, costPerUnit);
        }

        return true;
    }

    private void stopSellingItem(String itemName, long currentPrice, long costPerUnit, long lossAmount) {
        stoppedItems.add(itemName);

        String message = String.format(
                "🛑 STOP SELLING [%s]\n" +
                "Current Price: %d | Cost: %d\n" +
                "Loss: -%d\n" +
                "Action: Item held in order until price recovers",
                itemName, currentPrice, costPerUnit, Math.abs(lossAmount)
        );

        logAlert(message);

        if (webhook != null) {
            webhook.logEvent("ERROR",
                    "🛑 STOP SELLING **" + itemName + "** — Loss: **" + Math.abs(lossAmount) + "** | " +
                    "Current: " + currentPrice + " | Cost: " + costPerUnit);
        }
    }

    private void alertLowMargin(String itemName, double marginPercent, long currentPrice, long costPerUnit) {
        String message = String.format(
                "⚠️ LOW MARGIN [%s]\n" +
                "Margin: %.2f%% (threshold: 3%%)\n" +
                "Current Price: %d | Cost: %d\n" +
                "Recommendation: Hold or sell to AH?",
                itemName, marginPercent, currentPrice, costPerUnit
        );

        logAlert(message);

        if (webhook != null) {
            webhook.logEvent("DETECT",
                    "⚠️ LOW MARGIN **" + itemName + "** — " + String.format("%.2f%%", marginPercent) +
                    " | Current: " + currentPrice + " | Cost: " + costPerUnit);
        }
    }

    /**
     * Kiểm tra balance lệch (giảm) so với dự tính
     */
    public void checkBalanceDeviation(long expectedProfit, long actualBalance) {
        long expectedBalance = calculateExpectedBalance(expectedProfit);

        // Nếu actual < expected (bị lỗ) → DỪNG BÁN
        if (actualBalance < expectedBalance) {
            long deficit = expectedBalance - actualBalance;
            double deficitPercent = (deficit * 100.0) / expectedBalance;

            if (deficitPercent > 5.0) { // > 5% mới alert
                alertBalanceDeficit(expectedBalance, actualBalance, deficit);
            }
        }
    }

    private void alertBalanceDeficit(long expectedBalance, long actualBalance, long deficit) {
        String message = String.format(
                "💸 BALANCE DEFICIT DETECTED\n" +
                "Expected: %d | Actual: %d\n" +
                "Deficit: -%d\n" +
                "Possible causes: Failed sales, unreported losses, theft",
                expectedBalance, actualBalance, deficit
        );

        logAlert(message);

        if (webhook != null) {
            webhook.logEvent("ERROR",
                    "💸 BALANCE DEFICIT — Expected: " + expectedBalance +
                    " | Actual: " + actualBalance + " | Loss: " + deficit);
        }
    }

    /**
     * Restart: xóa danh sách stopped items
     */
    public void resetSafety() {
        stoppedItems.clear();
        itemCostBasis.clear();
        logAlert("✅ Safety checks reset - ready to resume selling");
    }

    /**
     * Check xem item có bị stop không
     */
    public boolean isItemStopped(String itemName) {
        return stoppedItems.contains(itemName);
    }

    /**
     * Kiểm tra hết chi tiết trước khi bán
     */
    public boolean performFullSafetyCheck(String itemName, long currentPrice, long quantity, long currentBalance) {
        // 1. Check margin + profit
        if (!canSellItem(itemName, currentPrice, quantity)) {
            return false;
        }

        // 2. Check balance
        checkBalanceDeviation(quantity * currentPrice, currentBalance);

        // 3. Bán được
        return true;
    }

    private long calculateExpectedBalance(long expectedProfit) {
        // TODO: Lấy từ BalanceChecker
        return expectedProfit;
    }

    private void logAlert(String message) {
        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);
            String log = String.format("[%s] %s\n\n", timestamp, message);

            Files.createDirectories(SAFETY_LOG.getParent());
            Files.write(SAFETY_LOG, log.getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to log safety alert: " + e.getMessage());
        }
    }

    public Set<String> getStoppedItems() {
        return new HashSet<>(stoppedItems);
    }
}
