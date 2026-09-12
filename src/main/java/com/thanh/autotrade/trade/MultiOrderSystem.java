package com.thanh.autotrade.trade;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Multi-Order System:
 * - Mỗi item có nhiều order từ buyers khác nhau
 * - Mỗi 10p scan lại AH, so sánh margin
 * - Margin >= 8% → bán; 5-8% nếu thanh khoản tốt; < 5% → hold trong order
 */
public class MultiOrderSystem {
    private static final Path ORDERS_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("autotrade-orders.json");
    private static final Gson GSON = new Gson();
    private static final int SCAN_INTERVAL_TICKS = 12000; // 10 phút
    private int scanCountdown = SCAN_INTERVAL_TICKS;

    private Map<String, List<Order>> itemOrders = new HashMap<>();

    public static class Order {
        public String itemName;
        public int quantity;
        public long pricePerUnit;
        public String buyerName;
        public long totalCost;
        public boolean fulfilled;

        public Order(String itemName, int quantity, long pricePerUnit, String buyerName) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.buyerName = buyerName;
            this.totalCost = quantity * pricePerUnit;
            this.fulfilled = false;
        }
    }

    public void tick() {
        scanCountdown--;
        if (scanCountdown <= 0) {
            scanAndUpdateOrders();
            scanCountdown = SCAN_INTERVAL_TICKS;
        }
    }

    public void addOrder(Order order) {
        itemOrders.computeIfAbsent(order.itemName, k -> new ArrayList<>()).add(order);
        saveOrders();
    }

    public void removeOrder(String itemName, String buyerName) {
        List<Order> orders = itemOrders.get(itemName);
        if (orders != null) {
            orders.removeIf(o -> o.buyerName.equals(buyerName));
            if (orders.isEmpty()) {
                itemOrders.remove(itemName);
            }
            saveOrders();
        }
    }

    private void scanAndUpdateOrders() {
        // TODO: Lấy giá AH hiện tại từ auto-trade.json hoặc cache
        // Tính margin, so sánh với order, quyết định bán/hold

        for (Map.Entry<String, List<Order>> entry : itemOrders.entrySet()) {
            String itemName = entry.getKey();
            List<Order> orders = entry.getValue();

            if (orders.isEmpty()) continue;

            // Lấy giá order đầu tiên (cost basis)
            Order firstOrder = orders.get(0);
            long costPerUnit = firstOrder.pricePerUnit;

            // TODO: Lấy giá AH hiện tại (cần từ AutoTradeConfig hoặc scan)
            // long ahPrice = getCurrentAHPrice(itemName);
            // long margin = ahPrice - costPerUnit;
            // double marginPercent = (margin * 100.0) / costPerUnit;

            // if (marginPercent >= 8.0) {
            //     sellToOrder(itemName, orders);
            // } else if (marginPercent >= 5.0 && isLiquidityGood(itemName)) {
            //     sellToOrder(itemName, orders);
            // } else {
            //     holdInOrder(itemName, orders);
            // }
        }
    }

    private void sellToOrder(String itemName, List<Order> orders) {
        for (Order order : orders) {
            if (!order.fulfilled) {
                // TODO: Execute sale to order.buyerName
                order.fulfilled = true;
            }
        }
        saveOrders();
    }

    private void holdInOrder(String itemName, List<Order> orders) {
        // Giữ lại item, ko bán AH
        // TODO: Log "Holding items - awaiting better margin"
    }

    private boolean isLiquidityGood(String itemName) {
        // TODO: Check supply > X (tùy item)
        return true;
    }

    public void saveOrders() {
        try {
            JsonObject root = new JsonObject();
            JsonArray itemsArray = new JsonArray();

            for (Map.Entry<String, List<Order>> entry : itemOrders.entrySet()) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("name", entry.getKey());

                JsonArray ordersArray = new JsonArray();
                for (Order order : entry.getValue()) {
                    JsonObject orderObj = new JsonObject();
                    orderObj.addProperty("buyer", order.buyerName);
                    orderObj.addProperty("quantity", order.quantity);
                    orderObj.addProperty("pricePerUnit", order.pricePerUnit);
                    orderObj.addProperty("fulfilled", order.fulfilled);
                    ordersArray.add(orderObj);
                }
                itemObj.add("orders", ordersArray);
                itemsArray.add(itemObj);
            }
            root.add("items", itemsArray);

            Files.createDirectories(ORDERS_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(ORDERS_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to save orders: " + e.getMessage());
        }
    }

    public void loadOrders() {
        if (!Files.exists(ORDERS_FILE)) return;

        try (Reader r = Files.newBufferedReader(ORDERS_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root != null && root.has("items")) {
                JsonArray items = root.getAsJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    JsonObject itemObj = items.get(i).getAsJsonObject();
                    String itemName = itemObj.get("name").getAsString();
                    JsonArray orders = itemObj.getAsJsonArray("orders");

                    for (int j = 0; j < orders.size(); j++) {
                        JsonObject orderObj = orders.get(j).getAsJsonObject();
                        Order order = new Order(
                                itemName,
                                orderObj.get("quantity").getAsInt(),
                                orderObj.get("pricePerUnit").getAsLong(),
                                orderObj.get("buyer").getAsString()
                        );
                        order.fulfilled = orderObj.get("fulfilled").getAsBoolean();
                        addOrder(order);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[AutoTrade] Failed to load orders: " + e.getMessage());
        }
    }

    public List<Order> getOrdersByItem(String itemName) {
        return itemOrders.getOrDefault(itemName, new ArrayList<>());
    }

    public Map<String, List<Order>> getAllOrders() {
        return itemOrders;
    }
}
