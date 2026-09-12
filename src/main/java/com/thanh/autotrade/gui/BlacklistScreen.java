package com.thanh.autotrade.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Blacklist GUI Screen
 * Hiển thị, thêm, xóa player từ blacklist
 */
public class BlacklistScreen extends Screen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    public BlacklistScreen() {
        super(Text.literal("AutoTrade Blacklist Manager"));
    }

    @Override
    protected void init() {
        // TODO: Implement blacklist GUI
        // Cần thêm:
        // - List view để hiển thị blacklist
        // - Text field để nhập player name
        // - Button "Add"
        // - Button "Remove" (select item rồi click)
        // - Button "Done"
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
