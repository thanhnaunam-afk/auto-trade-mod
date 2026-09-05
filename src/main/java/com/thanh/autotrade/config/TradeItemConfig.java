package com.thanh.autotrade.config;

public class TradeItemConfig {
    /** Tên item để gõ vào ô search trong /order hoặc /ah — dùng đúng tên hiển thị trong game, vd "Totem of Undying". */
    public String itemSearchName;

    public Mode mode = Mode.SELL_AH;

    /** Khi số lượng item này trong túi >= ngưỡng này thì mod mới xử lý. */
    public int triggerAmount = 1;

    public enum Mode {
        /** Cầm item, tự /ah sell <giá tính toán>. */
        SELL_AH,
        /** Tự mở /order, tạo order mua item này (dùng khi bạn muốn "đứng ra mua rẻ" chứ không phải bán). */
        CREATE_BUY_ORDER
    }

    public TradeItemConfig() {}

    public TradeItemConfig(String itemSearchName, Mode mode, int triggerAmount) {
        this.itemSearchName = itemSearchName;
        this.mode = mode;
        this.triggerAmount = triggerAmount;
    }
}
