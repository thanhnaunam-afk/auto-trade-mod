package com.thanh.autotrade.trade;

import com.thanh.autotrade.config.AutoTradeConfig;
import com.thanh.autotrade.config.TradeItemConfig;
import com.thanh.autotrade.util.ChatBuffer;
import com.thanh.autotrade.util.InventoryHelper;
import com.thanh.autotrade.util.PriceParser;
import com.thanh.autotrade.util.ScreenUtil;
import com.thanh.autotrade.util.ScriptRunner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongConsumer;

/**
 * Vòng lặp chính: quét túi đồ, item nào đủ ngưỡng thì đẩy vào ScriptRunner để chạy tuần
 * tự (mở GUI -> search -> đọc median giá nhiều slot -> tính margin/size theo balance &
 * cung -> gõ order/bán theo lô nhỏ có rescan).
 *
 * SKELETON — slot index, cách nhập text, và việc tooltip giá là "mỗi đơn vị" hay "tổng
 * cả stack" đều là giả định/CHƯA xác nhận trên server thật của bạn. Dùng `/autotrade
 * calibrate` để soi slot thật trước khi tin tưởng chạy `/autotrade start`.
 */
public class TradeStateMachine {
    private final AutoTradeConfig config;
    private final ScriptRunner runner = new ScriptRunner();
    private boolean running = false;
    private final Set<String> processedThisCycle = new HashSet<>();
    private int scanCooldown = 0;

    public TradeStateMachine(AutoTradeConfig config) {
        this.config = config;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        running = true;
        processedThisCycle.clear();
        runner.clear();
        reportChat("Bật auto trade. " + config.items.size() + " item đang theo dõi.");
    }

    public void stop() {
        running = false;
        runner.clear();
        reportChat("Đã tắt auto trade.");
    }

    public void tick() {
        if (!running) return;

        if (runner.isErrored()) {
            reportChat("Lỗi: " + runner.getLastError() + " — đã dừng auto, kiểm tra lại config rồi /autotrade start lại.");
            running = false;
            return;
        }

        runner.tick();

        if (runner.isIdle()) {
            if (scanCooldown > 0) {
                scanCooldown--;
                return;
            }
            TradeItemConfig next = findNextItemToProcess();
            if (next == null) {
                processedThisCycle.clear();
                scanCooldown = config.cycleCooldownTicks;
                return;
            }
            processedThisCycle.add(next.itemSearchName);
            queueTaskFor(next, 0);
        }
    }

    // ---------------------------------------------------------------- scan

    private TradeItemConfig findNextItemToProcess() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;
        for (TradeItemConfig item : config.items) {
            if (processedThisCycle.contains(item.itemSearchName)) continue;
            int count = InventoryHelper.countByName(mc, item.itemSearchName);
            if (count >= item.triggerAmount) return item;
        }
        return null;
    }

    private void queueTaskFor(TradeItemConfig item, int batchesDoneSoFar) {
        if (item.mode == TradeItemConfig.Mode.SELL_AH) {
            buildSellAhScript(item, batchesDoneSoFar);
        } else {
            buildCreateOrderScript(item);
        }
    }

    // ---------------------------------------------------------------- SELL_AH

    private void buildSellAhScript(TradeItemConfig item, int batchesDoneSoFar) {
        long[] ahMedian = {-1};

        pushReadMarketStats("ah", config.ahSearchSignSlotIndex, config.ahFilterFunnelSlotIndex,
                config.ahFilterClicksToLowestPrice, item.itemSearchName, "auction", true,
                (price, supply) -> ahMedian[0] = price);

        runner.push(r -> {
            if (ahMedian[0] <= 0) {
                r.fail("Không có giá AH hợp lệ cho " + item.itemSearchName);
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            int idx = InventoryHelper.findMainInventoryIndex(mc, item.itemSearchName);
            if (idx < 0) {
                reportChat(item.itemSearchName + " hết trong túi, dừng bán lượt này.");
                return;
            }
            if (idx <= 8) {
                InventoryHelper.selectHotbarSlot(mc, idx);
                r.waitTicks(config.delayAfterClickSlot);
            } else {
                mc.setScreen(new InventoryScreen(mc.player));
                r.waitTicks(config.delayAfterOpenGui);
                runner.pushFirst(rr -> {
                    InventoryHelper.holdItem(mc, item.itemSearchName, 8);
                    ScreenUtil.closeScreen();
                    rr.waitTicks(config.delayAfterClickSlot);
                });
            }
        });

        runner.push(r -> {
            long sellPrice = Math.round(ahMedian[0] * (1 - config.sellUndercutPercent / 100.0));
            if (sellPrice < 1) sellPrice = 1;
            ScreenUtil.sendCommand("ah sell " + sellPrice);
            reportChat("Bán 1 lô " + item.itemSearchName + " giá " + sellPrice + " (median AH: " + ahMedian[0] + ")");
            r.waitTicks(config.delayAfterTypeText);
        });

        // Kiểm tra xem có bị lỗi "đầy slot AH" không, rồi quyết định có bán tiếp lô nữa
        // trong cùng chu kỳ này hay để dành sang lần quét sau.
        runner.push(r -> {
            if (chatMentionsFullSlots()) {
                reportChat("Có vẻ đã đầy slot AH (hoặc lỗi tương tự) — dừng bán " + item.itemSearchName + " lượt này, thử lại chu kỳ sau.");
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            int remaining = InventoryHelper.countByName(mc, item.itemSearchName);
            int nextBatch = batchesDoneSoFar + 1;
            if (remaining > 0 && nextBatch < config.maxSellBatchesPerCycle) {
                queueTaskFor(item, nextBatch);
            }
        });
    }

    // ---------------------------------------------------------------- CREATE_BUY_ORDER

    private void buildCreateOrderScript(TradeItemConfig item) {
        long[] ahMedian = {-1};
        int[] ahSupply = {0};
        long[] existingOrderMedian = {-1};
        long[] balance = {-1};

        pushReadMarketStats("ah", config.ahSearchSignSlotIndex, config.ahFilterFunnelSlotIndex,
                config.ahFilterClicksToLowestPrice, item.itemSearchName, "auction", true,
                (price, supply) -> { ahMedian[0] = price; ahSupply[0] = supply; });

        pushReadMarketStats("order", config.orderSearchSignSlotIndex, config.orderFilterFunnelSlotIndex,
                config.orderFilterClicksToMostPaid, item.itemSearchName, "order", false,
                (price, supply) -> existingOrderMedian[0] = price);

        pushReadBalance(b -> balance[0] = b);

        runner.push(r -> {
            if (ahMedian[0] <= 0) {
                r.fail("Không có giá AH hợp lệ cho " + item.itemSearchName);
                return;
            }
            if (balance[0] <= 0) {
                reportChat("Không đọc được số dư — bỏ qua tạo order cho " + item.itemSearchName + " lượt này.");
                return;
            }

            Long price = computeOrderPrice(ahMedian[0], existingOrderMedian[0] > 0 ? existingOrderMedian[0] : null);
            if (price == null) {
                reportChat("Bỏ qua " + item.itemSearchName + ": lời sau phí dưới ngưỡng " + config.minProfitPercentAfterFee + "%, không đáng làm.");
                return;
            }

            long quantity = computeOrderQuantity(balance[0], price, ahSupply[0]);
            if (quantity <= 0) {
                reportChat("Bỏ qua " + item.itemSearchName + ": số lượng tính ra = 0 (hết tiền hoặc cung quá thấp).");
                return;
            }

            pushSendCommand("order");
            runner.push(rr -> {
                if (!ScreenUtil.isScreenTitleContains("order")) {
                    rr.fail("Không mở được GUI /order.");
                }
            });
            pushClickSlot(config.orderCreateChestSlotIndex);
            pushTypeText(item.itemSearchName);
            pushClickSuggestion(true);
            pushTypeText(String.valueOf(quantity));
            pushTypeText(String.valueOf(price));
            long finalQuantity = quantity;
            runner.push(rr -> reportChat("Tạo order mua " + item.itemSearchName + " x" + finalQuantity
                    + " giá " + price + "/đơn vị (AH median: " + ahMedian[0] + ", order khác: "
                    + (existingOrderMedian[0] > 0 ? existingOrderMedian[0] : "không có")
                    + ", số dư: " + balance[0] + ")"));
        });
    }

    private Long computeOrderPrice(long ahMedian, Long existingOrderMedian) {
        long floorFromMargin = Math.round(ahMedian * (1 - config.marginMaxPercent / 100.0));
        long ceiling = Math.round(ahMedian * (1 - config.marginMinPercent / 100.0));
        long floor = existingOrderMedian != null ? Math.max(existingOrderMedian, floorFromMargin) : floorFromMargin;
        if (floor > ceiling) return null;
        long price = ceiling;
        if (price <= 0) return null;

        // Lời thật = bán lại được giá AH sau khi trừ phí sàn, trừ đi giá vốn (price) đã mua.
        double netSellValue = ahMedian * (1 - config.ahFeePercent / 100.0);
        double profitPercent = ((netSellValue - price) / (double) price) * 100.0;
        if (profitPercent < config.minProfitPercentAfterFee) return null;

        return price;
    }

    private long computeOrderQuantity(long balance, long orderPrice, int visibleSupply) {
        if (orderPrice <= 0) return 0;
        long byBalance = (long) Math.floor(balance * (config.balanceSpendCapPercent / 100.0) / orderPrice);
        long qty = byBalance;
        if (config.applySupplyCap) {
            long bySupply = (long) Math.floor(visibleSupply * (config.supplyCapPercent / 100.0));
            qty = Math.min(qty, Math.max(bySupply, 0));
        }
        qty = Math.min(qty, config.absoluteMaxQuantityPerAction);
        return Math.max(qty, 0);
    }

    // ---------------------------------------------------------------- primitives dùng chung

    /** Đọc median giá + tổng số lượng của priceSampleSlotCount slot liên tiếp, để chống outlier/troll-list. */
    private void pushReadMarketStats(String command, int searchSignSlot, int filterSlot, int filterClicks,
                                      String itemName, String expectedTitleSubstring, boolean requireFound,
                                      java.util.function.BiConsumer<Long, Integer> onStats) {
        pushSendCommand(command);
        runner.push(r -> {
            if (!ScreenUtil.isScreenTitleContains(expectedTitleSubstring)) {
                r.fail("Không mở được GUI '" + command + "' (title không chứa '" + expectedTitleSubstring + "').");
            }
        });
        pushClickSlot(searchSignSlot);
        pushTypeText(itemName);
        pushClickSuggestion(requireFound);
        for (int i = 0; i < filterClicks; i++) {
            pushClickSlot(filterSlot);
        }
        runner.push(r -> {
            List<Long> prices = new ArrayList<>();
            int totalCount = 0;
            for (int i = 0; i < config.priceSampleSlotCount; i++) {
                int slot = config.firstResultItemSlotIndex + i;
                ItemStack stack = ScreenUtil.getSlotStack(slot);
                if (stack.isEmpty()) continue;
                List<String> lines = ScreenUtil.getSlotTooltipLines(slot);
                var parsed = PriceParser.findPriceInTooltip(lines);
                if (parsed.isPresent()) {
                    long unitPrice = config.assumeTooltipPriceIsPerUnit
                            ? parsed.get()
                            : Math.round(parsed.get() / (double) Math.max(1, stack.getCount()));
                    prices.add(unitPrice);
                    totalCount += stack.getCount();
                }
            }
            if (prices.isEmpty()) {
                if (requireFound) r.fail("Không đọc được giá nào cho " + itemName + " trong " + command);
                return;
            }
            onStats.accept(PriceParser.median(prices), totalCount);
        });
        runner.push(r -> {
            ScreenUtil.closeScreen();
            r.waitTicks(config.delayAfterClickSlot);
        });
    }

    private void pushReadBalance(LongConsumer onBalance) {
        runner.push(r -> {
            ScreenUtil.sendCommand("balance");
            r.waitTicks(config.delayAfterOpenGui);
        });
        runner.push(r -> {
            List<String> lines = ChatBuffer.snapshotNewestFirst();
            for (String line : lines) {
                var v = PriceParser.findPriceInTooltip(List.of(line));
                if (v.isPresent()) {
                    onBalance.accept(v.get());
                    return;
                }
            }
            // Không fail cứng — để nơi gọi tự quyết định bỏ qua lượt này, tránh dừng cả bot
            // chỉ vì 1 lần đọc /balance lỡ trượt.
        });
    }

    private boolean chatMentionsFullSlots() {
        if (config.fullSlotErrorKeyword == null || config.fullSlotErrorKeyword.isBlank()) return false;
        String needle = config.fullSlotErrorKeyword.toLowerCase();
        for (String line : ChatBuffer.snapshotNewestFirst()) {
            if (line.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    private void pushClickSuggestion(boolean requireFound) {
        runner.push(r -> {
            ItemStack suggestion = ScreenUtil.getSlotStack(config.searchSuggestionResultSlotIndex);
            if (suggestion.isEmpty()) {
                if (requireFound) r.fail("Không tìm thấy gợi ý item sau khi search.");
                return;
            }
            ScreenUtil.leftClickSlot(config.searchSuggestionResultSlotIndex);
            r.waitTicks(config.delayAfterClickSlot);
        });
    }

    private void pushSendCommand(String command) {
        runner.push(r -> {
            ScreenUtil.sendCommand(command);
            r.waitTicks(config.delayAfterOpenGui);
        });
    }

    private void pushClickSlot(int slotIndex) {
        runner.push(r -> {
            ScreenUtil.leftClickSlot(slotIndex);
            r.waitTicks(config.delayAfterClickSlot);
        });
    }

    private void pushTypeText(String text) {
        runner.push(r -> {
            // Mặc định gửi qua chat. Nếu server bạn mở SignEditScreen thật (không phải chat
            // prompt), cần thay đoạn này bằng gói UpdateSignC2SPacket tương ứng.
            ScreenUtil.sendChatLine(text);
            r.waitTicks(config.delayAfterTypeText);
        });
    }

    private void reportChat(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[AutoTrade] " + message), false);
        }
    }
}
