package com.thanh.autotrade.gui;

import com.thanh.autotrade.AutoTradeMod;
import com.thanh.autotrade.config.AutoTradeConfig;
import com.thanh.autotrade.trade.TradeStateMachine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Mở bằng phím tắt (mặc định "]") thay vì gõ lệnh trong chat. Chỉ chứa các nút
 * KHÔNG cần một GUI khác (như /order, /ah) đang mở cùng lúc — vì mở Screen này sẽ
 * đóng bất kỳ container nào đang mở. calibrate/chatdump vẫn phải là lệnh chat vì
 * chúng cần chạy trong lúc /order hoặc /ah đang mở, không thể dùng Screen riêng cho việc đó.
 */
public class AutoTradeMenuScreen extends Screen {
    private final AutoTradeConfig config;
    private final TradeStateMachine stateMachine;
    private final AutoTradeMod mod;

    public AutoTradeMenuScreen(AutoTradeConfig config, TradeStateMachine stateMachine, AutoTradeMod mod) {
        super(Text.literal("AutoTrade"));
        this.config = config;
        this.stateMachine = stateMachine;
        this.mod = mod;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 46;

        ButtonWidget toggleButton = ButtonWidget.builder(toggleLabel(), btn -> {
            if (stateMachine.isRunning()) {
                stateMachine.stop();
            } else {
                stateMachine.start();
            }
            btn.setMessage(toggleLabel());
        }).dimensions(centerX - 100, top, 200, 20).build();
        addDrawableChild(toggleButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("Nạp lại config (autotrade.json)"), btn -> {
            mod.reloadConfig();
        }).dimensions(centerX - 100, top + 24, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Đóng"), btn -> close())
                .dimensions(centerX - 100, top + 48, 200, 20).build());
    }

    private Text toggleLabel() {
        return Text.literal(stateMachine.isRunning() ? "Dừng AutoTrade" : "Bật AutoTrade");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int top = this.height / 2 - 46;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top - 34, 0xFFFFFF);
        String status = "Item theo dõi: " + config.items.size() + "  |  Đang chạy: "
                + (stateMachine.isRunning() ? "CÓ" : "KHÔNG");
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), centerX, top - 18, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        // Không pause game khi mở menu này (vì cần nó hoạt động trong lúc chơi bình thường).
        return false;
    }
}
