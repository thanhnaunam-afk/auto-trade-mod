package com.thanh.autotrade.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Evade Status GUI
 * Hiển thị detected players, detection count
 */
public class EvadeStatusScreen extends Screen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    public EvadeStatusScreen() {
        super(Text.literal("AutoTrade Evade Detection"));
    }

    @Override
    protected void init() {
        // TODO: Implement evade status GUI
        // Hiển thị:
        // - Detected players list
        // - Detection count per player
        // - Last detection time
        // - Cooldown timer
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
