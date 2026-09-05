package com.thanh.autotrade.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Dùng slot mapping CHUẨN của vanilla PlayerScreenHandler (ổn định qua các version,
 * không cần calibrate như GUI của server):
 *   0        = crafting output
 *   1-4      = crafting input
 *   5-8      = armor
 *   9-35     = túi đồ chính (27 ô)
 *   36-44    = hotbar (9 ô)
 *   45       = offhand
 */
public final class InventoryHelper {
    private InventoryHelper() {}

    public static int countByName(MinecraftClient mc, String displayName) {
        if (mc.player == null) return 0;
        int total = 0;
        for (ItemStack stack : mc.player.getInventory().getMainStacks()) {
            if (!stack.isEmpty() && stack.getName().getString().equalsIgnoreCase(displayName)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** index 0-8 = hotbar, 9-35 = túi chính, trong mảng PlayerInventory.main (size 36). Trả -1 nếu không có. */
    public static int findMainInventoryIndex(MinecraftClient mc, String displayName) {
        if (mc.player == null) return -1;
        var main = mc.player.getInventory().getMainStacks();
        for (int i = 0; i < main.size(); i++) {
            ItemStack stack = main.get(i);
            if (!stack.isEmpty() && stack.getName().getString().equalsIgnoreCase(displayName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Đảm bảo item đang cầm trên tay (hotbar slot được chọn) là displayName.
     * Nếu item đang nằm trong túi chính (không phải hotbar), cần MỞ inventory screen
     * (nhấn phím 'e') TRƯỚC khi gọi hàm này, vì thao tác swap slot chỉ hoạt động khi
     * có 1 HandledScreen đang mở.
     *
     * @return true nếu đã cầm đúng item (hoặc đã swap thành công), false nếu không tìm thấy.
     */
    public static boolean holdItem(MinecraftClient mc, String displayName, int preferredHotbarSlot) {
        int idx = findMainInventoryIndex(mc, displayName);
        if (idx < 0) return false;

        if (idx <= 8) {
            // đã ở hotbar rồi — chỉ cần chọn đúng ô
            selectHotbarSlot(mc, idx);
            return true;
        }

        // đang ở túi chính -> cần GUI mở để swap. Người gọi (TradeStateMachine) chịu trách
        // nhiệm đảm bảo inventory screen đang mở trước bước này.
        var screen = ScreenUtil.currentHandledScreen();
        if (screen == null) return false;
        int screenSlot = idx; // slot 9-35 trong PlayerScreenHandler trùng index 9-35 của main[]
        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, screenSlot, preferredHotbarSlot, SlotActionType.SWAP, mc.player);
        selectHotbarSlot(mc, preferredHotbarSlot);
        return true;
    }

    public static void selectHotbarSlot(MinecraftClient mc, int hotbarSlot) {
        mc.player.getInventory().setSelectedSlot(hotbarSlot);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
        }
    }
}
