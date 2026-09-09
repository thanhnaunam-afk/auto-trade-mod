package com.thanh.autotrade.gui;

import com.thanh.autotrade.AutoTradeMod;
import com.thanh.autotrade.config.AutoTradeConfig;
import com.thanh.autotrade.config.AdvancedConfig;
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
    private final AdvancedConfig advancedConfig;

    public AutoTradeMenuScreen(AutoTradeConfig config, TradeStateMachine stateMachine, AutoTradeMod mod) {
        super(Text.literal("AutoTrade Menu"));
        this.config = config;
        this.stateMachine = stateMachine;
        this.mod = mod;
        this.advancedConfig = AdvancedConfig.load();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 60;

        // Nút Start/Stop
        ButtonWidget toggleButton = ButtonWidget.builder(toggleLabel(), btn -> {
            if (stateMachine.isRunning()) {
                stateMachine.stop();
            } else {
                stateMachine.start();
            }
            btn.setMessage(toggleLabel());
        }).dimensions(centerX - 100, top, 200, 20).build();
        addDrawableChild(toggleButton);

        // Nút Reload Config
        addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Nạp lại config (autotrade.json)"), btn -> {
            mod.reloadConfig();
        }).dimensions(centerX - 100, top + 25, 200, 20).build());

        // Nút Settings
        addDrawableChild(ButtonWidget.builder(Text.literal("⚙️ Settings (Webhook + API)"), btn -> {
            this.client.setScreen(new SettingsScreen(this, advancedConfig));
        }).dimensions(centerX - 100, top + 50, 200, 20).build());

        // Nút Đóng
        addDrawableChild(ButtonWidget.builder(Text.literal("❌ Đóng"), btn -> close())
                .dimensions(centerX - 100, top + 75, 200, 20).build());
    }

    private Text toggleLabel() {
        return Text.literal(stateMachine.isRunning() ? "⏹️ Dừng AutoTrade" : "▶️ Bật AutoTrade");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int top = this.height / 2 - 60;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top - 30, 0xFFFFFF);
        String status = "Item theo dõi: " + config.items.size() + "  |  Đang chạy: "
                + (stateMachine.isRunning() ? "✓ CÓ" : "✗ KHÔNG");
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), centerX, top - 15, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        // Không pause game khi mở menu này (vì cần nó hoạt động trong lúc chơi bình thường).
        return false;
    }
}
