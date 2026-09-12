package com.thanh.autotrade.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Safety Status GUI
 * Hiển thị: stopped items, margin alerts, balance status
 */
public class SafetyStatusScreen extends Screen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    public SafetyStatusScreen() {
        super(Text.literal("AutoTrade Safety Status"));
    }

    @Override
    protected void init() {
        // TODO: Implement safety status GUI
        // Hiển thị:
        // - Stopped items list
        // - Last balance + profit
        // - Low margin alerts
        // - Button "Reset Safety"
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
