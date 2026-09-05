package com.thanh.autotrade.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Đọc / thao tác GUI đang mở (Orders, AH...) mà server gửi xuống dưới dạng
 * HandledScreen bình thường (chest GUI). Nếu server dùng GUI custom kiểu khác
 * (packet riêng, không phải ScreenHandler chuẩn) thì lớp này KHÔNG áp dụng được —
 * cần kiểm tra bằng lệnh /autotrade calibrate trước khi tin tưởng số slot.
 *
 * LƯU Ý MAPPING: các tên method (getTooltip, clickSlot...) đúng theo Yarn mappings
 * quãng 1.21.x. Nếu Loom báo lỗi biên dịch, khả năng cao API đã đổi tên nhẹ giữa
 * các bản — chỉnh lại theo gợi ý autocomplete của IDE là chạy được.
 */
public final class ScreenUtil {
    private ScreenUtil() {}

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static HandledScreen<?> currentHandledScreen() {
        if (mc().currentScreen instanceof HandledScreen<?> hs) {
            return hs;
        }
        return null;
    }

    public static boolean isScreenTitleContains(String needleLowercase) {
        HandledScreen<?> hs = currentHandledScreen();
        if (hs == null) return false;
        String title = hs.getTitle().getString().toLowerCase();
        return title.contains(needleLowercase.toLowerCase());
    }

    public static String currentScreenTitle() {
        HandledScreen<?> hs = currentHandledScreen();
        return hs == null ? null : hs.getTitle().getString();
    }

    public static ItemStack getSlotStack(int slotIndex) {
        HandledScreen<?> hs = currentHandledScreen();
        if (hs == null) return ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= hs.getScreenHandler().slots.size()) return ItemStack.EMPTY;
        return hs.getScreenHandler().getSlot(slotIndex).getStack();
    }

    /** Trả về các dòng tooltip (name + lore) dạng text thường, để regex giá ra khỏi đó. */
    public static List<String> getSlotTooltipLines(int slotIndex) {
        List<String> lines = new ArrayList<>();
        ItemStack stack = getSlotStack(slotIndex);
        if (stack.isEmpty()) return lines;
        List<Text> tooltip = stack.getTooltip(
                Item.TooltipContext.DEFAULT,
                mc().player,
                TooltipType.BASIC
        );
        for (Text t : tooltip) {
            lines.add(t.getString());
        }
        return lines;
    }

    /** Click chuột trái bình thường vào 1 slot trong GUI đang mở. */
    public static void leftClickSlot(int slotIndex) {
        HandledScreen<?> hs = currentHandledScreen();
        if (hs == null) return;
        var handler = hs.getScreenHandler();
        mc().interactionManager.clickSlot(handler.syncId, slotIndex, 0, SlotActionType.PICKUP, mc().player);
    }

    public static void closeScreen() {
        if (mc().player != null) {
            mc().player.closeHandledScreen();
        }
    }

    /** Gõ 1 dòng vào chat — dùng cho luồng "search/sign" nếu server bắt qua chat message. */
    public static void sendChatLine(String text) {
        if (mc().player != null && mc().getNetworkHandler() != null) {
            mc().getNetworkHandler().sendChatMessage(text);
        }
    }

    public static void sendCommand(String commandNoSlash) {
        if (mc().player != null && mc().getNetworkHandler() != null) {
            mc().player.networkHandler.sendChatCommand(commandNoSlash);
        }
    }
}
